# Payment Transaction Processor

Este proyecto implementa el endpoint de pagos solicitado utilizando una arquitectura serverless nativa en AWS. El objetivo principal fue construir un sistema reactivo y orientado a eventos que pueda escalar automáticamente ante picos de demanda.

# Decisiones de Diseño

### 1. Base de Datos
Para este reto decidí utilizar Amazon DynamoDB en lugar de un motor relacional tradicional como PostgreSQL. La principal limitante que suele presentarse al trabajar con AWS Lambda en un entorno de alta concurrencia es el agotamiento del pool de conexiones a la base de datos (Connection Exhaustion). DynamoDB en cambio, se comunica a través de HTTP y escala en modo de pago por solicitud (PAY_PER_REQUEST), lo que encaja de forma perfecta con el modelo de computación altamente efímero de las Lambdas. Además, DynamoDB ofrece tiempos de respuesta muy predecibles a gran escala, resultando idóneo para el procesamiento asíncrono.

### 2. Estrategia de Concurrencia
Para garantizar la integridad transaccional (que no se pierda dinero en transacciones simultáneas), implementé una estrategia multicapa con bloqueos y control lógico:
- En primer lugar, aplico una validación de idempotencia súper rápida desde la memoria local. Implementé el motor Caffeine Cache dentro de la propia Lambda, por lo que rechazo cualquier intento duplicado (mismo payment_id en cortos fragmentos de tiempo) en submilisegundos y sin generar consultas bloqueantes en base de datos.
- En segundo lugar, y respecto a los saldos, los registros en base de datos dependen de Control de Concurrencia Optimista (Optimistic Locking) nativo. Cada actualización necesita incrementar un atributo de versión lógica. Si dos hilos logran evitar la primera defensa y procuran alterar simultáneamente el mismo balance, la base de datos rechace la transacción más leve.
- En tercer lugar, uso librerías de tolerancia de fallos (Resilience4j) a nivel del código sobre las excepciones. Si experimenta la violación optimista referida anteriormente, el servicio atrapa ese resultado fallido y desencadena reintentos automáticos para retomar el proceso de la solicitud original sobre las versiones saneadas, absorbiendo estas resoluciones y manteniendo el flujo de la solicitud limpio de cara al usuario final.

### 3. Explicación del Diagrama de Arquitectura de Alta Demanda
A continuación presento la estructura de la arquitectura final desplegada para funcionar bajo presión de una demanda escalada.

graph TD
    Client[Aplicativo o Frontend] -->|Llamada HTTP POST| APIGateway[Amazon API Gateway] [1 millon de peticiones gratis]
    APIGateway -->|Invocador| Lambda[AWS Lambda: PaymentProcessor]
    
    Lambda <-->|Filtro Idempotente Directo| Caffeine[(Caffeine In-Memory L1 Cache)]
    Lambda <-->|Repositorio de Persistencia| DynamoDB[(Amazon DynamoDB)]
    Lambda -->|Disparador de Evento Integrado| SQS[Amazon SQS]
    
    SQS -->|Consumido por| Workers[Consumidores Diferidos: Reportes / Alertas / Notificaciones]


Justificación conceptual general de componentes involucrados:

- Amazon API Gateway: Cumple las funciones del Balanceador. Intercepta los Requests desde afuera blindando el tráfico. Expone validaciones superficiales y descifra el flujo directamente contra la aplicación sin necesidad de mantener instancias vivas (como haría Nginx o un Load Balancer clásico en EC2).
- AWS Lambda (con Java 21 LTS y modelo WebFlux): Supone el trabajo central pero bajo la capa de reactivismo puramente computacional. Levantan hasta donde lo mande la demanda paralela. WebFlux impide bloqueamientos de Hilo (Thread Blocking) cuando Lambda llama a procesadores externos, liberando la memoria drásticamente.
- Amazon DynamoDB: Actúa como origen único de base de consistencia fuerte sin el cuello de botella tradicional de conectividad, escalando mediante request y persistiendo en diferentes zonas geográficas dentro del servicio AWS de manera transparente.
- Amazon SQS (Cola de Eventos): Adhiere integración elástica a los sistemas secundarios. En vez de que esos métodos impacten en el tiempo real sincrónico sobre el proceso de pago conllevado, el código solo deja el objeto en la cola publicándolo y despacha la respuesta HTTP finalizada a los de adelante, de forma que el resto de los sistemas del negocio tomen de allí cuando los necesiten.


# Despliegue de Entorno

Gracias al acercamiento del componente "Infrastructure as Code" todo se lanza programaticamente con el Serverless Framework en la terminal hacia sus cuentas nube validadas. No necesitas levantar dependencias previas localmente para su testeo real:

Compilar versión transaccional empaquetando JAR
bash
./mvnw clean compile package -Plambda

Publicar recurso final a AWS
bash
serverless deploy

Todos los recursos, integrando API Gateway, Lambda, Tablas de entorno DynamoDB y la Cola SQS son administrados e instanciados inmediatamente desde un mismo despliegue.
