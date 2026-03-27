# React Payment Frontend 🚀

Este es el frontend moderno para el reto **Transaction Processor**, construido con las mejores prácticas de la industria.

## 🛠️ Stack Técnico
- **React 18 + TypeScript**: Tipado fuerte para evitar errores en producción.
- **Vite**: El bundler más rápido y moderno del ecosistema.
- **Tailwind CSS**: Para un diseño "premium" con utilidades limpias.
- **Framer Motion**: Animaciones fluidas para el scanner y los overlays.
- **Axios**: Cliente HTTP para conectar con el backend WebFlux.

## 🧱 Arquitectura y Principios
Seguí principios de **Limpio (Clean Code)** y **SOLID**:
1.  **Separación de Responsabilidades**: 
    - `src/services/`: Lógica de API aislada.
    - `src/hooks/`: Lógica de negocio y estado encapsulada en `usePayment`.
    - `src/components/`: UI pura y reutilizable.
2.  **Productividad**: Uso de composición de componentes.
3.  **UX**: Scanner animado y feedback en tiempo real.

## 🚀 Cómo ejecutarlo
Desde esta carpeta (`frontend/`):

```bash
# 1. Instalar dependencias
npm install

# 2. Correr en modo desarrollo
npm run dev
```

El frontend buscará el backend en `http://localhost:8083`. Asegúrate de tener el backend de Spring Boot corriendo primero.
