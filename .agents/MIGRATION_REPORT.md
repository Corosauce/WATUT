# REPORT DI MIGRAZIONE & CHANGELOG COMPLETO: PORTING A MINECRAFT 26.2 (FABRIC)

Questo report documenta in dettaglio tutte le modifiche, correzioni, refactoring e risoluzioni di bug eseguiti durante l'aggiornamento e la migrazione del progetto a **Minecraft 26.2 su Fabric Loader**.

---

## 1. Aggiornamenti dell'Infrastruttura di Build (Gradle, Loom, Java)

### `CoroUtil/build.gradle` & `gradle.properties`:
- Aggiornato Java target e source compatibility a **Java 25** (`release = 25`).
- Aggiornato Fabric Loom a `1.17.19` (`1.17-SNAPSHOT`).
- Configurate le versioni target:
  - `minecraft_version = 26.2`
  - `loader_version = 0.16.0+` (testato con `0.19.3`)
  - `fabric_version = 0.157.0+26.2`
  - `coroutil_version = 1.3.8`
- Rimosse completamente le dipendenze da `fuzs.forgeconfigapiport` e `com.electronwill.night-config`.
- Rimossi i vecchi package `loader/forge` e `loader/neoforge` dall'albero dei sorgenti.

### `WATUT/build.gradle` & `gradle.properties`:
- Aggiornato Java target e source compatibility a **Java 25** (`release = 25`).
- Aggiornato Fabric Loom a `1.17.19`.
- Collegata la dipendenza su `CoroUtil` compilato localmente:
  ```groovy
  implementation "com.corosus.coroutil:CoroUtil:${project.minecraft_version}.0-${project.coroutil_version}"
  ```
- Rimosse le dipendenze da `forgeconfigapiport` e `night-config`.
- Rimosso il package `loader/neoforge`.
- Risolto il conflitto di risorse duplicate per `assets/minecraft/atlases/particles.json` pulendo `src/generated/resources`.

---

## 2. Refactoring del Sistema di Configurazione (`CoroUtil`)

### File Modificati / Riscritto:
- [`ModConfigDataFabric.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/CoroUtil/src/main/java/com/corosus/coroutil/loader/fabric/ModConfigDataFabric.java):
  - **Problema Iniziale**: Dipendeva da `net.minecraftforge.fml.config.IConfigSpec` e `ForgeConfigAPIPort`, causando all'avvio: `java.lang.NoClassDefFoundError: net/minecraftforge/fml/config/IConfigSpec`.
  - **Soluzione**: Riscritto completamente come parser/writer nativo e leggero in puro Java NIO. Legge e scrive direttamente i file `.toml`/properties in `FabricLoader.getInstance().getConfigDir()`.
- [`ConfigModFabric.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/CoroUtil/src/main/java/com/corosus/coroutil/loader/fabric/ConfigModFabric.java):
  - Rimosso l'uso di `net.neoforged.fml.config.ConfigTracker`.
  - Implementato `reloadConfigs` con iterazione sicura su `CoroConfigRegistry.instance().configs`.
- [`MultiLoaderUtil.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/CoroUtil/src/main/java/com/corosus/coroutil/util/MultiLoaderUtil.java):
  - Semplificato per restituire direttamente `ModConfigDataFabric` su Fabric senza invocazioni riflettive.
- [`fabric.mod.json`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/CoroUtil/src/main/resources/fabric.mod.json):
  - Aggiornato target a `"minecraft": ">=26.2"` e `"java": ">=25"`.

---

## 3. Aggiornamenti delle API e Fix di Compatibilità (`WATUT`)

### A. Metodi NBT (`CompoundTag`)
Minecraft 26.2 ha reso obbligatori i metodi tipizzati `get<Type>Or`:
- In [`ConfigServerSyncHelper.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/config/ConfigServerSyncHelper.java): convertiti tutti i getter a `getBooleanOr`, `getIntOr`, `getDoubleOr`, `getStringOr`.
- In [`PacketNBTFromServer.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/network/PacketNBTFromServer.java): convertito `tag.getString` a `tag.getStringOr("command_name", "")`.
- In [`WatutModFabricClient.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/loader/fabric/WatutModFabricClient.java): convertito `tag.getString` a `tag.getStringOr("command_name", "")`.
- In [`PlayerStatusManagerServer.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/PlayerStatusManagerServer.java): aggiornati i getter NBT di player status (`getIntOr`, `getFloatOr`, `getStringOr`, `getBooleanOr`).
- In [`PlayerStatusManagerClient.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/PlayerStatusManagerClient.java): aggiornato il parsing dei pacchetti NBT e il recupero dei byte array con `tag.getByteArray("pixelData").orElse(new byte[0])`.

### B. Accesso a Screen & GUI
- In [`PlayerStatusManagerClient.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/PlayerStatusManagerClient.java):
  - Creato metodo `getCurrentScreen()` che accede in sicurezza a `Minecraft.getInstance().gui.screen()`.
  - Sostituite tutte le verifiche su `mc.screen` con `getCurrentScreen()`.

### C. Textures e Rendering
- In [`RenderHelper.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/screen/RenderHelper.java):
  - Aggiornato costruttore `DynamicTexture` con etichetta identificativa: `new DynamicTexture("watut_screen", width, height, true)`.
- In [`ParticleDynamic.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/particle/ParticleDynamic.java):
  - Aggiornati i costruttori per la compatibilità con il layer `SingleQuadParticle.Layer.TRANSLUCENT`.

---

## 4. Risoluzione dei Bug di Rendering e Animazione In-Game

### A. Mancata Comparsa della GUI 3D all'Apertura degli Inventari
- **Diagnosi**: Nel metodo `tickOtherPlayerClient` in `PlayerStatusManagerClient.java`, la condizione di creazione delle particelle tentava prima il rendering dinamico e, non trovando un render-type valido, saltava la creazione delle particelle statiche LoD.
- **Correzione**: Implementato il fallback automatico su `ParticleStaticLoD` per tutte le schermate del gioco (`INVENTORY`, `CHEST`, `CRAFTING`, `ESCAPE`, `ANVIL`, `FURNACE`, `BREWING_STAND`, `ENCHANTING_TABLE`, ecc.).

### B. Visibilità della GUI da Dietro (Double-Sided Rendering)
- **Diagnosi**: I quad delle particelle venivano cullerati quando visualizzati dal retro (ad esempio quando un giocatore osserva un altro giocatore da dietro o sopra la spalla).
- **Correzione**: In [`ParticleRotating.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/particle/ParticleRotating.java), il metodo `extract` ora genera sia il quad frontale che un quad posteriore ruotato di 180° su Y (`backQuaternion = quaternion.mul(Axis.YP.rotationDegrees(180))`), garantendo visibilità a 360°.

### C. Bug del Movimento del Ciuffo / Cappello (`hat`) e delle Maniche (`sleeves`)
- **Diagnosi**: In Minecraft 26.2, `hat` è registrato come nodo figlio (`child`) di `head`, e le maniche sono nodi figli delle braccia. Il vecchio codice applicava manualmente `hat.xRot = head.xRot` e `hat.setPos(...)`, causando una **doppia rotazione (velocità 2x)** e uno sfasamento spaziale che faceva volare via i rilievi 3D della skin.
- **Correzione**: In [`PlayerStatusManagerClient.setupRotationsHook`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/PlayerStatusManagerClient.java), rimosse tutte le manipolazioni ridondanti dei nodi figli. La gerarchia nativa del modello propaga ora in modo 100% naturale le rotazioni e le posizioni della testa e delle braccia ai rispettivi strati esterni della skin.

---

---

## 5. Implementazione del Live Dynamic Screen & Risoluzione Bug Critici

### A. Risoluzione Crash Nativo `EXCEPTION_ACCESS_VIOLATION` in `jemalloc.dll`
- **Diagnosi**: 
  1. Nel vecchio `RenderHelper.java`, venivano usate chiamate a `MemoryUtil.memAlloc`/`memCopy` verso il puntatore di `NativeImage`.
  2. In [`ScreenData.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/screen/ScreenData.java), il metodo `freeTexturePixelData()` invocava `MemoryUtil.memFree(texturePixelData)` e `MemoryUtil.memFree(decompressionBuffer)` quando un giocatore chiudeva la GUI o durante il `cleanup()`. Poiché i buffer erano gestiti dallo heap standard Java (`ByteBuffer.wrap`), `jemalloc` tentava di deallocare indirizzi non validi nel runtime C++, causando un crash irreversibile istantaneo con codice `0xc0000005`.
- **Correzione**:
  1. Rimosso ogni riferimento e chiamata a `MemoryUtil` da tutto il progetto. Ora il ciclo di vita dei buffer e delle immagini è gestito al 100% in Java sicuro e tramite le API ufficiali di Minecraft (`NativeImage.getPixelBytes()`, `DynamicTexture.close()`).
  2. In [`DynamicScreenManager.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/screen/DynamicScreenManager.java), la cattura dello schermo sfrutta la pipeline asincrona nativa e GPU-safe di Minecraft 26.2: `Screenshot.takeScreenshot(...)` con ridimensionamento pulito via `resizeSubRectTo(...)`.

### E. Raddrizzamento GUI Posteriore, Culling Unilaterale Antighosting e Digitazione
1. **Culling Unilaterale Dinamico e Raddrizzamento Perfetto (Zero Sovrapposizioni)**:
   - **Problema**: Disegnando sia la faccia frontale che quella posteriore nello stesso spazio con trasparenza, le due texture traslucide si sovrapponevano visivamente, generando un effetto "fantasma/doppio" in cui si intravedeva la faccia opposta.
   - **Soluzione**: In [`DynamicScreenRenderer.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/screen/DynamicScreenRenderer.java), il renderer calcola la posizione angolare della telecamera (`toCamera.dot(frontNormal)`):
     - **Se l'osservatore è davanti**: viene disegnata **esclusivamente la faccia frontale** (nitida al 100%, senza interferenze).
     - **Se l'osservatore è dietro**: viene disegnata **esclusivamente la faccia posteriore raddrizzata** (leggibile normalmente da sinistra a destra, senza alcuna inversione a specchio).
     - **Zero sovrapposizione**: eliminata ogni possibilità di ghosting o interferenza tra le due facce.
2. **Digitazione nell'Incudine (`AnvilScreen`)**:
   - In [`KeyboardHandlerKeyPress.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/mixin/client/KeyboardHandlerKeyPress.java), aggiunto il gancio su `charTyped` per inviare istantaneamente ogni carattere digitato a Minecraft e ai listener di stato.
   - Aggiunto `AnvilScreen` in [`watut.accesswidener`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/resources/watut.accesswidener) e [`InputTracker.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/input/InputTracker.java) (`isGuiFocusedOnTextBox` ed `extractTextFromScreen`).
3. **Ripristino Fumetto Animato e Overlay Chat ("Player is typing...")**:
   - In [`PlayerStatusClientManager.java`](file:///C:/Users/Alessio/Desktop/SERVER%20minecraft/mod%20minecraft/test%20mod/unofficialfork/WATUT/src/main/java/com/corosus/watut/client/status/PlayerStatusClientManager.java), separata la gestione delle particelle di chat/testo (`CHAT_TYPING` / `CHAT_IDLE`) dalla texture dinamica dei container.
   - Implementato `renderChatTypingOverlay` agganciato a `ScreenRenderWithTooltip` per mostrare la notifica testuale di digitazione in tempo reale sopra la barra della chat.

---

## 6. Stato dei Componenti del Progetto

| Componente | Stato | Note |
|---|---|---|
| **CoroUtil Build** | ✅ Completo | Compilato con Fabric Loom 1.17.19 e Java 25 |
| **CoroUtil Config** | ✅ Completo | Sistema di configurazione nativo Fabric `.toml` |
| **WATUT Build** | ✅ Completo | Compilato e testato con successo |
| **Animazioni Braccia / Puntamento** | ✅ Funzionante | Puntamento mouse e movimenti in tempo reale |
| **Animazioni Digitazione Chat** | ✅ Funzionante | Testo, velocità e animazione braccia |
| **Livelli 3D Skin (Ciuffo/Maniche)** | ✅ Risolto | Ancoraggio perfetto senza doppia rotazione |
| **GUI 3D Olografica Statica** | ✅ Funzionante | Visualizzazione LoD a doppia faccia (front/back) |
| **Live Dynamic Screen 3D** | ✅ Stabile | Streaming sicuro GPU via `Screenshot.takeScreenshot` |
| **Stabilità Memoria JVM / JNI** | ✅ Risolto | Eliminati puntatori grezzi jemalloc, zero crash |
| **Stabilità Multiplayer / Singleplayer** | ✅ Stabile | Nessun crash o dipendenza da Forge |


