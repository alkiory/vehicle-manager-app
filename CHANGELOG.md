# Registro de cambios

Todos los cambios notables en la aplicación Vehicle Manager se documentarán en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto se adhiere a [Control de Versiones Semántico](https://semver.org/lang/es/).

## [No publicado]

### Corregido
- Recordatorios: programación diaria persistente, autocorrección al iniciar la app y registro visible de la última comprobación.

## [0.3.0] - 2026-03-07

### Añadido
- Gestión de permisos de notificaciones y estado visible en la pantalla de Ajustes para los recordatorios de servicio.
- Changelog público en la raíz del repositorio para mayor transparencia y trazabilidad.

### Cambiado
- Ajustes: los cambios ahora se guardan explícitamente con el botón "Guardar cambios" y sobreviven al cerrar la aplicación.
- Estadísticas: unidades visibles, cálculo y redondeo corregidos, €/L en lugar de ¢/L, y cada cifra explicada con una breve descripción.

### Corregido
- Recordatorios: programación fiable mediante tareas periódicas y reprogramación automática tras reiniciar el dispositivo.

## [0.2.2] - 2026-03-05

### Añadido
- Aviso de cambios sin guardar al salir de los formularios de nuevo repostaje y nuevo servicio.

### Corregido
- Al salir de un formulario sin cambios, ahora vuelve a la lista en lugar de quedar abierto en segundo plano.

## [0.2.1] - 2026-03-04

### Cambiado
- Rediseño visual con la paleta "Medianoche con calor" y degradados, en modo claro y oscuro.
- Barras superior e inferior de navegación más compactas para evitar superposiciones.

### Corregido
- Dígitos invertidos al escribir números en los campos de antelación de Ajustes.
- Textos que rompían la interfaz en la pantalla de Ajustes.
- Mensajes de guardado e importación que no desaparecían automáticamente.
- Versión mostrada dinámicamente en el apartado "Acerca de".

## [0.1.0] - 2026-03-03

### Añadido
- Motor de predicción de consumo y estimador de próximo repostaje.
- Seguimiento de permisos de notificaciones y recordatorios de servicio.
- Icono adaptativo y pantalla de inicio mejorada.
- Persistencia de datos local optimizada frente al reinicio del dispositivo.

### Corregido
- Cálculo del coste por kilómetro ajustado para mostrar euros por kilómetro.
- Sincronización del tema visual entre modo automático, claro y oscuro.
