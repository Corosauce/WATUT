# AGENT CONTEXT & TECHNICAL REFERENCE: CoroUtil & WATUT (Minecraft 26.2 Fabric)

Questo documento fornisce tutto il contesto tecnico, le decisioni architetturali, la struttura del codice, i dettagli del rendering, del networking, del sistema di configurazione e le istruzioni di build per chiunque debba proseguire o mantenere lo sviluppo di **CoroUtil** e **WATUT** su **Minecraft 26.2 (Fabric)**.

---

## 1. Ambito di Progetto & Stack Tecnologico

- **Target Game Version**: `Minecraft 26.2`
- **Target Mod Loader**: `Fabric Loader >= 0.16.0` (testato con `0.19.3`)
- **API**: `Fabric API 0.157.0+26.2`
- **Java Runtime**: `Java 25` (Azul Systems JDK 25.0.2 / Eclipse Temurin / OpenJDK 25, `release = 25`)
- **Build Tool**: `Gradle 9.7` con `Fabric Loom 1.17.19` (configurato con SNAPSHOT fallback)
- **Dipendenze Loader**: **FABRIC ONLY**. Qualsiasi dipendenza o importazione residua da MinecraftForge, NeoForge o ForgeConfigAPIPort è stata rimossa ed è vietata.

---

## 2. Architettura & Relazione tra Mod

Il workspace è diviso in due sottomoduli:
```
unofficialfork/
├── CoroUtil/                 # Libreria core di utilità, configurazione e registri
│   ├── src/main/java/
│   │   ├── com/corosus/coroutil/
│   │   └── com/corosus/modconfig/
│   ├── src/main/resources/
│   └── build.gradle
├── WATUT/                    # Mod "What Are They Up To" (animazioni, GUI 3D, chat status)
│   ├── src/main/java/
│   │   ├── com/corosus/watut/
│   ├── src/main/resources/
│   └── build.gradle
└── .agents/                  # Documentazione e report per agenti AI
    ├── CONTEXT.md
    ├── ARCHITECTURE.md
    └── MIGRATION_REPORT.md
```

### Relazione di Build:
1. `WATUT` dipende direttamente dal JAR compilato di `CoroUtil` via Maven Local:
   ```groovy
   implementation "com.corosus.coroutil:CoroUtil:${project.minecraft_version}.0-${project.coroutil_version}"
   ```
2. **Regola fondamentale di compilazione**: ogni modifica apportata a `CoroUtil` richiede l'esecuzione di:
   ```powershell
   cd CoroUtil
   .\gradlew publishToMavenLocal build
   ```
   prima di poter compilare `WATUT`.

---

## 3. Modifiche Chiave delle API Minecraft 26.2

### A. Modifiche NBT (`CompoundTag`)
In Minecraft 26.2 tutti i metodi getter diretti deprecati di `CompoundTag` (`getInt`, `getString`, `getBoolean`, `getFloat`, `getByteArray`, `getCompound`) sono stati rimpiazzati con versioni sicure:
- `tag.getIntOr(key, defaultValue)`
- `tag.getFloatOr(key, defaultValue)`
- `tag.getStringOr(key, defaultValue)`
- `tag.getBooleanOr(key, defaultValue)`
- `tag.getByteArray(key).orElse(new byte[0])`
- `tag.getCompoundOrEmpty(key)`

### B. Gestione Schermate Client (`Screen`)
- `Minecraft.getInstance().screen` è privato o protetto da getter.
- In `PlayerStatusManagerClient.java` viene impiegato l'helper dedicato:
  ```java
  public Screen getCurrentScreen() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gui != null && mc.gui.screen() != null) {
          return mc.gui.screen();
      }
      return null;
  }
  ```

### C. Textures Dinamiche (`DynamicTexture`)
- In Minecraft 26.2 il costruttore di `DynamicTexture` richiede l'etichetta identificativa `String`:
  ```java
  new DynamicTexture("watut_screen", width, height, true);
  ```

---

## 4. Sistema di Configurazione Nativo Fabric (`CoroUtil`)

### Problema Risolto:
Nelle versioni legacy, `CoroUtil` utilizzava `ForgeConfigAPIPort` per emulare le configurazioni di Forge su Fabric (`IConfigSpec`, `ConfigTracker`, `ForgeConfigSpec`). Su Fabric 26.2 puro questo causava un crash immediato all'avvio: `NoClassDefFoundError: net/minecraftforge/fml/config/IConfigSpec`.

### Implementazione Attuale:
- [`ModConfigDataFabric.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/CoroUtil/src/main/java/com/corosus/coroutil/loader/fabric/ModConfigDataFabric.java): Implementazione nativa, zero-dependency basata su `FabricLoader.getInstance().getConfigDir().resolve(saveFilePath + ".toml")`.
- Legge e scrive direttamente file di configurazione formattati in stile TOML/properties leggibili dall'utente, supportando `String`, `Integer`, `Double`, `Boolean` e annotazioni `@ConfigComment` e `@ConfigParams`.
- [`MultiLoaderUtil.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/CoroUtil/src/main/java/com/corosus/coroutil/util/MultiLoaderUtil.java): Istanzia direttamente `ModConfigDataFabric` senza reflection rischiosa o controlli di loader terzi.

---

## 5. Rendering del Modello Giocatore & Livelli Skin (Hat / Sleeves)

### Dettaglio Critico sulla Gerarchia di `ModelPart` in 26.2:
In Minecraft 26.2, `PlayerModel` ha una struttura ad albero a nodi gerarchici:
- `hat` è registrato come `child` di `head` (`head.getChild("hat")`).
- `left_sleeve` è un `child` di `left_arm`.
- `right_sleeve` è un `child` di `right_arm`.
- `jacket` è un `child` di `body`.

### ⚠️ Regola Fondamentale di Animazione:
Poiché `hat` e `sleeves` sono nodi figli, ereditano **automaticamente** le rotazioni e le traslazioni applicate ai nodi genitori (`head`, `leftArm`, `rightArm`).
**NON** assegnare mai manualmente `playerModel.hat.xRot = playerModel.head.xRot` o `playerModel.hat.setPos(...)`: farlo causerebbe una **doppia trasformazione (velocità 2x e dislocazione nello spazio)** facendo volare via il ciuffo e i rilievi 3D della skin.

In [`setupRotationsHook`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/PlayerStatusManagerClient.java):
- Modificare unicamente `playerModel.head` (per inclinare lo sguardo verso l'inventario o l'animazione idle) e `playerModel.leftArm` / `playerModel.rightArm` (per puntamento mouse o digitazione tastiera).
- I livelli secondari (`hat`, `leftSleeve`, `rightSleeve`) seguiranno perfettamente il modello.

---

## 6. Sistema di Rendering delle Particelle 3D (GUI Hologram & Chat)

### A. Registrazione Atlas (`particles.json`)
Per fare in modo che le texture di WATUT (`assets/watut/textures/particles/*.png`) siano caricate nello sprite atlas delle particelle di Minecraft 26.2, è configurato:
- [`src/generated/resources/assets/minecraft/atlases/particles.json`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/generated/resources/assets/minecraft/atlases/particles.json) con tutte le voci `watut:particles/<nome_sprite>`.

### B. Double-Sided Quad Rendering
Di default, i quad delle particelle in Minecraft sono sottoposti a culling del retro.
Per permettere la visualizzazione della schermata 3D olografica da qualsiasi angolazione (da davanti al giocatore o da dietro guardando sopra la spalla), [`ParticleRotating.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/particle/ParticleRotating.java) estrae sia il quad frontale che quello posteriore:
```java
this.extractRotatedQuad(renderState, camera, quaternion, partialTicks);

Quaternionf backQuaternion = new Quaternionf(quaternion);
backQuaternion.mul(Axis.YP.rotationDegrees(180));
this.extractRotatedQuad(renderState, camera, backQuaternion, partialTicks);
```

### C. Live Dynamic 3D GUI Hologram (Completamente Implementato & GPU-Safe)
- **Live Screen Streaming**: Invece di semplici icone statiche LoD, WATUT implementa uno streaming visivo reale in 3D:
  - [`DynamicScreenManager.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/screen/DynamicScreenManager.java) cattura la schermata in modo asincrono con `Screenshot.takeScreenshot(...)` GPU-safe.
  - [`AbstractContainerScreenAccessor.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/mixin/client/AbstractContainerScreenAccessor.java) calcola il Bounding Box esatto e l'Aspect Ratio naturale di qualsiasi container aperto (casse singole/doppie, fornaci, enderchest, anvil, mod).
  - [`DynamicScreenRenderer.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/screen/DynamicScreenRenderer.java) effettua il rendering con culling direzionale unilaterale (zero sovrapposizioni e vista posteriore raddrizzata/non-specchiata).
  - Buffer GPU persistente 256x256 che garantisce una `GpuTextureView` sempre valida, azzerando i crash di deallocazione al cambio di interfaccia.
- **Supporto Digitazione & Chat**:
  - `KeyboardHandlerKeyPress` aggancia `charTyped` per consentire la digitazione fluida in EditBox (come nell'incudine `AnvilScreen`).
  - Fumetto animato di digitazione con puntini (`CHAT_TYPING` / `CHAT_IDLE`) e notifica in chat `"<Player> is typing..."` tramite `renderChatTypingOverlay`.

---

## 7. Comandi di Compilazione Rapida

Per compilare e generare i file `.jar` finali:

```powershell
# 1. Compilazione e pubblicazione locale di CoroUtil
cd "C:\Users\Alessio\Desktop\SERVER minecraft\mod minecraft\test mod\unofficialfork\CoroUtil"
.\gradlew clean publishToMavenLocal build

# 2. Compilazione di WATUT
cd "C:\Users\Alessio\Desktop\SERVER minecraft\mod minecraft\test mod\unofficialfork\WATUT"
.\gradlew clean build
```

I jar compilati e pronti all'uso vengono generati in:
- `CoroUtil/build/libs/coroutil-fabric-26.2.0-1.3.8.jar`
- `WATUT/build/libs/watut-fabric-26.2.0-1.2.7.jar`

