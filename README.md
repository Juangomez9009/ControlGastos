# ControlGastos

Aplicación Android nativa en Kotlin para registrar usuarios, iniciar sesión y administrar gastos personales mediante un CRUD completo. Los datos se guardan localmente con Room y cada usuario consulta únicamente sus gastos.

Proyecto académico: **App Android con Login/registro y CRUD**.

## Funcionalidades

- Registro con nombre, correo único, contraseña y confirmación.
- Inicio y cierre de sesión con mensajes de error y éxito.
- Creación, listado, detalle, edición y eliminación de gastos.
- Confirmación antes de eliminar y actualización automática del listado.
- Gastos con descripción, monto, categoría y fecha.
- Persistencia al cerrar y volver a abrir la aplicación.
- Formularios con desplazamiento, soporte de orientación vertical/horizontal y conservación de campos durante la rotación.
- Etiquetas permanentes, controles de al menos 48dp y colores adaptados a los temas claro y oscuro en los elementos verificados.

No requiere servidor, Firebase ni una cuenta de un servicio externo. La aplicación funciona sin conexión; la primera sincronización del proyecto necesita Internet para descargar las dependencias.

## Requisitos y versiones

| Componente | Configuración del proyecto |
|---|---|
| Lenguaje | Kotlin con soporte integrado del Android Gradle Plugin |
| SDK mínimo | API 24 |
| SDK de compilación | API 37 |
| SDK objetivo | API 37 |
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.6.0 |
| Room | 2.8.4 |
| KSP | 2.3.10 |
| RecyclerView | 1.3.2 |
| Lifecycle | 2.8.7 |
| Compatibilidad del código Java | Java 11 |
| JDK usado para ejecutar Gradle | JetBrains Runtime / OpenJDK 25.0.3, incluido en Android Studio |

**Entorno probado:** Android Studio, compilación `262.9437.185.2621.16467767`, en Windows. Usar una versión de Android Studio compatible con AGP 9.4.1 y SDK 37; no es necesario que coincida exactamente el número de compilación del IDE.

> La compatibilidad Java 11 del código no significa que Gradle deba ejecutarse con JDK 11. En las pruebas se utilizó el JDK incluido en Android Studio, versión 25.0.3.

## Emulador utilizado

| Propiedad | Valor |
|---|---|
| Nombre visible | **Medium Phone API 37.0** |
| Perfil | Medium Phone |
| API | 37.0 |
| Imagen | Google Play (`google_apis_playstore`) |
| Arquitectura | x86_64 |
| Resolución | 1080 × 2400 |
| Densidad | 420 dpi |

Para reproducir este entorno, abrir **Tools → Device Manager → Create Device**, elegir el perfil Medium Phone y descargar una imagen Google Play de API 37 para x86_64. Se puede usar otro dispositivo compatible con el SDK mínimo; las verificaciones documentadas se realizaron en el emulador de la tabla.

## Abrir y ejecutar

1. Descargar el código desde el repositorio o descomprimir el ZIP.
2. En Android Studio, seleccionar **Open** y abrir la carpeta raíz `ControlGastos`, que contiene `settings.gradle.kts` y `gradlew.bat`. No abrir únicamente la carpeta `app`.
3. En **Tools → SDK Manager**, instalar Android SDK Platform 37 y, para el emulador, Android Emulator y la imagen de sistema indicada arriba. Aceptar las licencias y componentes que solicite el IDE.
4. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, seleccionar el JDK incluido en Android Studio que sea compatible con las versiones indicadas. El proyecto fue probado con JBR 25.0.3.
5. Ejecutar **Sync Project with Gradle Files** y esperar a que termine. También se puede localizar esta acción con **Ctrl + Shift + A**.
6. Iniciar el emulador desde **Device Manager**, o conectar un dispositivo físico con depuración USB.
7. Seleccionar la configuración **app**, elegir el dispositivo y pulsar **Run ▶**.

Android Studio configura la ubicación local del SDK en `local.properties`. Este archivo depende del equipo y no se incluye en el repositorio.

### Compilar desde la terminal

Con el SDK configurado y `JAVA_HOME` apuntando a un JDK compatible, ejecutar desde la raíz del proyecto:

```powershell
# Windows / PowerShell
.\gradlew.bat :app:assembleDebug
```

En macOS o Linux:

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Primer uso y flujo de demostración

La base de datos del emulador del desarrollador no forma parte del código fuente. **En una instalación nueva, crear una cuenta desde Registro.** No se publican credenciales personales ni existe un usuario predefinido.

1. En Login, pulsar **Regístrate**.
2. Completar nombre, un correo válido, contraseña de al menos seis caracteres y confirmación. Ejemplo de correo de prueba: `evaluacion@example.com`; elegir una contraseña propia.
3. Pulsar **Registrarse**. Aparece el mensaje de cuenta creada y se vuelve al Login.
4. Ingresar con las mismas credenciales. Se abre **Mis gastos**.
5. Pulsar **Añadir gasto** y completar, por ejemplo, descripción `Almuerzo`, monto `12500`, categoría `Alimentación` y una fecha.
6. Pulsar **Guardar gasto**. El registro aparece en el listado sin reiniciar.
7. Tocar el gasto para abrir su detalle; pulsar **Editar**, cambiar el monto a `15000` y guardar. El detalle y el listado reflejan el cambio.
8. Crear un segundo gasto de prueba y eliminarlo desde su detalle, confirmando la acción. Desaparece de la lista.
9. Pulsar **Cerrar sesión** y volver a ingresar: los gastos conservados siguen disponibles.

El cierre de sesión limpia la pila de navegación para evitar regresar a la lista mediante Atrás. No se implementa una opción «Recordarme» ni un gestor persistente de sesión independiente de la navegación de Android.

## Validaciones

| Formulario | Reglas |
|---|---|
| Registro | Nombre obligatorio, correo válido y único, contraseña no vacía de mínimo seis caracteres y confirmación coincidente. |
| Login | Correo válido y contraseña de mínimo seis caracteres; credenciales incorrectas generan un mensaje sin permitir el ingreso. |
| Gasto | Descripción obligatoria, monto mayor que cero, máximo dos decimales, categoría obligatoria y fecha válida mediante selector. |

Los correos se normalizan quitando espacios exteriores y convirtiéndolos a minúsculas. El monto admite `12500`, `12500.50` o `12500,50`, sin separadores de miles. Se almacena en centavos con tipo `Long` para evitar errores de representación decimal.

Al faltar categoría se muestra un aviso, se resalta el selector y se bloquea el guardado. El error desaparece al seleccionar una categoría. Cancelar un formulario no guarda los cambios; cancelar la confirmación de eliminación conserva el gasto.

## Arquitectura y persistencia

Se aplica **MVVM** para separar interfaz, estado/lógica de presentación y acceso a datos:

```text
Activities + diseños XML + GastoAdapter
                  ↓
              ViewModels
                  ↓
             Repositories
                  ↓
               DAOs / Room
```

- **Activities:** Login (`MainActivity`), Registro, Listado (`GastosActivity`), Detalle y Formulario Crear/Editar.
- **ViewModels:** mantienen el estado observable y coordinan validaciones y operaciones asíncronas.
- **Repositories:** encapsulan las operaciones de usuarios y gastos.
- **DAOs y RoomDatabase:** definen consultas, entidades, restricciones y persistencia.
- **Listado reactivo:** Room emite cambios mediante Flow; el ViewModel expone el estado a la interfaz con LiveData.

El código principal se encuentra en `app/src/main/java/com/example/controlgastos/`. Los diseños están en `app/src/main/res/layout/`, los modelos y DAOs en `data/local/`, los repositorios en `data/` y los ViewModels en `ui/`.

### Esquema de datos

```text
Usuario (1) ──────────── (N) Gasto
id                           id
nombre                       usuarioId → Usuario.id
correo [único]               descripcion
contrasenaHash               montoCentavos
contrasenaSalt               categoria
creadoEn                     fecha
```

- Contraseñas derivadas con PBKDF2-HMAC-SHA1, salt aleatorio por usuario, 210.000 iteraciones y salida de 256 bits. El login compara el hash derivado con el guardado; no se almacena la contraseña en texto.
- Base local `control_gastos.db`, versión 2.
- Migración 1 → 2: añade gastos y su índice conservando los usuarios existentes.
- Las consultas de lectura, edición y eliminación de gastos incluyen el identificador del propietario.
- Esquemas exportados de Room en `app/schemas/`.

Los datos permanecen en el dispositivo. Desinstalar la aplicación, borrar sus datos o usar **Wipe Data** en el emulador elimina el almacenamiento local; volver a compilar o instalar normalmente desde Android Studio conserva los datos si se mantiene la aplicación y su firma.

## Pruebas y verificaciones

Pruebas unitarias en `app/src/test/` e instrumentadas en `app/src/androidTest/`. Incluyen montos y fechas, autenticación, correos duplicados, persistencia, migración, CRUD, aislamiento por usuario, categoría obligatoria, rotación y navegación.

Para ejecutar las pruebas unitarias:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Las verificaciones finales de interfaz se realizaron en **API 37**, con fuente al **150 %**, temas claro y oscuro y orientación vertical/horizontal. La clase `VerificacionFinalTest` requiere configurar esa escala antes de ejecutar su prueba de accesibilidad y restaurarla al finalizar. No se afirma compatibilidad de estas pruebas de interfaz con todas las versiones del SDK mínimo.

Resultados comprobados:

- Conservación de descripción, monto, categoría y fecha al rotar y recrear los formularios.
- Regreso coherente con Atrás, retorno Registro → Login y cierre de sesión.
- Cancelación de eliminación sin pérdida del registro.
- Acceso mediante desplazamiento a Guardar y Cancelar en horizontal con fuente ampliada.
- Etiquetas permanentes, tamaños táctiles y contraste de los elementos examinados en ambos temas.

La revisión es de accesibilidad básica; no sustituye una auditoría integral con lector de pantalla ni pruebas en todos los dispositivos.

## Problemas de ejecución

- **Fallo de sincronización:** comprobar Internet, SDK 37, licencias y JDK seleccionado. Usar el Gradle Wrapper incluido en el proyecto.
- **Emulador bloqueado / «System UI isn't responding»:** esperar a que termine de iniciar. Si persiste, reiniciar mediante **Device Manager → menú del dispositivo → Cold Boot Now**. Cold Boot conserva los datos; Wipe Data los elimina.
- **Botones fuera de vista en horizontal:** desplazar el formulario. En el emulador, mantener pulsado el mouse y arrastrar hacia arriba o usar la rueda.
- **No recuerda credenciales:** crear otra cuenta de prueba con un correo distinto. El proyecto no incluye recuperación de contraseña ni permite leer la contraseña original desde el hash.

## Entrega académica

El código puede entregarse mediante repositorio o ZIP. Incluir el Gradle Wrapper, fuentes, recursos, configuración de compilación, esquemas Room y este README. Excluir todas las carpetas `build/`, `.gradle/`, configuración personal del IDE y `local.properties`.

El enunciado también solicita un **video de 3–5 minutos** que muestre el flujo completo y un **documento técnico de 1–2 páginas** con justificación de arquitectura, esquema de datos, persistencia y validaciones. Esos entregables se preparan por separado; este README no los sustituye.
