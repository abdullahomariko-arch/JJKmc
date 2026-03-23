package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class JJKCursedToolsPlugin extends JavaPlugin {

    private static JJKCursedToolsPlugin instance;

    private ConfigManager configManager;
    private PlayerDataStore playerDataStore;

    private TechniqueRegistry techniqueRegistry;
    private TechniqueManager techniqueManager;

    private CursedEnergyManager cursedEnergyManager;
    private CooldownManager cooldownManager;
    private RegenLockManager regenLockManager;
    private NullifyManager nullifyManager;

    private ItemIds itemIds;
    private CursedToolFactory cursedToolFactory;

    private ActionbarUI actionbarUI;
    private BossbarUI bossbarUI;

    private AbilityService abilityService;

    private CommandRouter commandRouter;

    // ===== Divine Wheel =====
    private WheelUI wheelUI;
    private PhenomenonDetector phenomenonDetector;
    private WheelTierManager wheelTierManager;
    private WheelCombatHandler wheelCombatHandler;
    private WheelStateSerializer wheelStateSerializer;

    // ===== Creation Technique =====
    private CreationManager creationManager;

    // ===== Cursed Speech =====
    private CursedSpeechManager cursedSpeechManager;

    // ===== Boogie Woogie =====
    private BoogieWoogieManager boogieWoogieManager;

    // ===== Playful Cloud =====
    private PlayfulCloudManager playfulCloudManager;

    // ===== Copy Technique =====
    private GlobalDataStore globalDataStore;
    private CopyManager copyManager;
    private RikaManager rikaManager;
    private CopyLifecycleListener copyLifecycleListener;
    private RikaStorageGUI rikaStorageGUI;
    private CursedBodyItem cursedBodyItem;
    private CopyListener copyListener;
    private CopyCTService copyCTService;

    // ===== Domain Expansion =====
    private DomainManager domainManager;

    // ===== Idle Death Gamble =====
    private IdleDeathGambleManager idleDeathGambleManager;
    private IdleDeathGambleListener idleDeathGambleListener;

    // ===== Projection Sorcery =====
    private BetterModelBridge betterModelBridge;
    private ProjectionVisuals projectionVisuals;
    private ProjectionFreezeHandler projectionFreezeHandler;
    private ProjectionManager projectionManager;
    private ProjectionListener projectionListener;

    // ===== Séance =====
    private SeanceManager seanceManager;

    // ===== Straw Doll =====
    private StrawDollManager strawDollManager;

    // ===== Ten Shadows =====
    private TenShadowsManager tenShadowsManager;
    private ShadowStorageGUI shadowStorageGUI;

    // ===== Limitless =====
    private LimitlessManager limitlessManager;

    // ===== Six Eyes Trait =====
    private SixEyesTrait sixEyesTrait;

    // ===== Blood Manipulation =====
    private BloodManipulationManager bloodManipulationManager;

    // ===== Deadly Sentencing =====
    private DeadlySentencingManager deadlySentencingManager;

    public static JJKCursedToolsPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        configManager.load();

        this.playerDataStore = new PlayerDataStore(this);

        this.techniqueRegistry = new TechniqueRegistry();
        registerTechniques();
        this.techniqueManager = new TechniqueManager(this, techniqueRegistry, playerDataStore);

        this.cooldownManager = new CooldownManager(this, playerDataStore);
        this.regenLockManager = new RegenLockManager(this, playerDataStore);
        this.nullifyManager = new NullifyManager(this, techniqueManager, playerDataStore);

        this.cursedEnergyManager = new CursedEnergyManager(this, playerDataStore);

        this.itemIds = new ItemIds(this);
        this.cursedToolFactory = new CursedToolFactory(this, itemIds);

        this.actionbarUI = new ActionbarUI(this);
        this.bossbarUI = new BossbarUI(this);

        this.abilityService = new AbilityService(
                this, configManager, techniqueManager, cursedEnergyManager,
                cooldownManager, regenLockManager, nullifyManager,
                cursedToolFactory, actionbarUI, bossbarUI
        );

        // ===== Divine Wheel =====
        this.wheelUI = new WheelUI(this);
        this.phenomenonDetector = new PhenomenonDetector(this);
        this.wheelTierManager = new WheelTierManager(this);
        this.wheelStateSerializer = new WheelStateSerializer(this);
        this.wheelCombatHandler = new WheelCombatHandler(this, phenomenonDetector, wheelTierManager, wheelUI);

        // ===== Creation Technique =====
        this.creationManager = new CreationManager(this);

        // ===== Cursed Speech =====
        this.cursedSpeechManager = new CursedSpeechManager(this);

        // ===== Boogie Woogie =====
        this.boogieWoogieManager = new BoogieWoogieManager(this);

        // ===== Playful Cloud =====
        this.playfulCloudManager = new PlayfulCloudManager(this);

        // ===== Copy Technique =====
        this.globalDataStore = new GlobalDataStore(this);
        this.copyManager = new CopyManager(this);
        this.rikaManager = new RikaManager(this);
        this.copyLifecycleListener = new CopyLifecycleListener(this);
        this.rikaStorageGUI = new RikaStorageGUI(this);
        this.cursedBodyItem = new CursedBodyItem(this);
        this.copyCTService = new CopyCTService(this);
        this.copyListener = new CopyListener(this, rikaManager, rikaStorageGUI, cursedBodyItem);

        // ===== Projection Sorcery =====
        this.betterModelBridge = new BetterModelBridge(this);
        this.projectionVisuals = new ProjectionVisuals(this);
        this.projectionFreezeHandler = new ProjectionFreezeHandler(this, betterModelBridge, projectionVisuals);
        this.projectionManager = new ProjectionManager(this, projectionVisuals, projectionFreezeHandler);
        this.projectionListener = new ProjectionListener(this, projectionManager);

        // ===== Domain Expansion =====
        this.domainManager = new DomainManager(this);

        // ===== Idle Death Gamble =====
        this.idleDeathGambleManager = new IdleDeathGambleManager(this);
        this.idleDeathGambleListener = new IdleDeathGambleListener(this, idleDeathGambleManager);

        // ===== Séance =====
        this.seanceManager = new SeanceManager(this);

        // ===== Straw Doll =====
        this.strawDollManager = new StrawDollManager(this);

        // ===== Ten Shadows =====
        this.tenShadowsManager = new TenShadowsManager(this);
        this.shadowStorageGUI = new ShadowStorageGUI(this);

        // ===== Six Eyes Trait =====
        this.sixEyesTrait = new SixEyesTrait(this);

        // ===== Limitless =====
        this.limitlessManager = new LimitlessManager(this);

        // ===== Blood Manipulation =====
        this.bloodManipulationManager = new BloodManipulationManager(this);

        // ===== Deadly Sentencing =====
        this.deadlySentencingManager = new DeadlySentencingManager(this);

        // ===== Listeners =====
        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this, playerDataStore, cursedEnergyManager, bossbarUI, actionbarUI), this);
        Bukkit.getPluginManager().registerEvents(new ToolUseListener(this, abilityService, cursedToolFactory), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this, abilityService, cursedToolFactory, regenLockManager, nullifyManager, wheelCombatHandler), this);

        Bukkit.getPluginManager().registerEvents(copyLifecycleListener, this);
        Bukkit.getPluginManager().registerEvents(rikaStorageGUI, this);
        Bukkit.getPluginManager().registerEvents(copyListener, this);

        Bukkit.getPluginManager().registerEvents(projectionListener, this);
        Bukkit.getPluginManager().registerEvents(new DomainListener(this), this);

        Bukkit.getPluginManager().registerEvents(new WheelDamageListener(wheelCombatHandler), this);
        Bukkit.getPluginManager().registerEvents(new CursedSpeechListener(this, cursedSpeechManager), this);
        Bukkit.getPluginManager().registerEvents(idleDeathGambleListener, this);
        Bukkit.getPluginManager().registerEvents(new SeanceListener(this, seanceManager), this);
        Bukkit.getPluginManager().registerEvents(new StrawDollListener(this, strawDollManager), this);

        Bukkit.getPluginManager().registerEvents(new TenShadowsListener(this, tenShadowsManager), this);
        // Ten Shadows scroll wheel selection
        Bukkit.getPluginManager().registerEvents(new TenShadowsScrollListener(this, tenShadowsManager), this);
        // Shadow Storage GUI
        Bukkit.getPluginManager().registerEvents(shadowStorageGUI, this);

        // Cursed Energy Progression
        Bukkit.getPluginManager().registerEvents(new CursedEnergyListener(this), this);

        // Limitless
        Bukkit.getPluginManager().registerEvents(new LimitlessListener(this, limitlessManager), this);

        // Blood Manipulation
        Bukkit.getPluginManager().registerEvents(new BloodManipulationListener(this, bloodManipulationManager), this);

        // ===== Commands =====
        this.commandRouter = new CommandRouter(this);
        commandRouter.registerDefaults();
        if (getCommand("jjk") != null) {
            getCommand("jjk").setExecutor(commandRouter);
            getCommand("jjk").setTabCompleter(commandRouter);
        }

        CmdWheel wheelCmd = new CmdWheel(this, wheelTierManager, wheelUI);
        if (getCommand("wheel") != null) {
            getCommand("wheel").setExecutor(wheelCmd);
            getCommand("wheel").setTabCompleter(wheelCmd);
        }

        CmdCreation creationCmd = new CmdCreation(this, creationManager);
        if (getCommand("creation") != null) {
            getCommand("creation").setExecutor(creationCmd);
            getCommand("creation").setTabCompleter(creationCmd);
        }

        if (getCommand("cursedspeach") != null) {
            CmdCursedSpeech csCmd = new CmdCursedSpeech(this, cursedSpeechManager);
            getCommand("cursedspeach").setExecutor(csCmd);
            getCommand("cursedspeach").setTabCompleter(csCmd);
        }

        if (getCommand("boogiewoogie") != null) {
            CmdBoogieWoogie bwCmd = new CmdBoogieWoogie(this, boogieWoogieManager);
            getCommand("boogiewoogie").setExecutor(bwCmd);
            getCommand("boogiewoogie").setTabCompleter(bwCmd);
        }

        if (getCommand("copy") != null) {
            CmdCopy copyCmd = new CmdCopy(this, rikaManager, copyCTService);
            getCommand("copy").setExecutor(copyCmd);
            getCommand("copy").setTabCompleter(copyCmd);
        } else {
            getLogger().warning("Command /copy is missing from plugin.yml");
        }

        if (getCommand("projection") != null) {
            CmdProjection projCmd = new CmdProjection(this);
            getCommand("projection").setExecutor(projCmd);
            getCommand("projection").setTabCompleter(projCmd);
        } else {
            getLogger().warning("Command /projection is missing from plugin.yml");
        }

        if (getCommand("domain") != null) {
            CmdDomain domainCmd = new CmdDomain(this);
            getCommand("domain").setExecutor(domainCmd);
            getCommand("domain").setTabCompleter(domainCmd);
        } else {
            getLogger().warning("Command /domain is missing from plugin.yml");
        }

        if (getCommand("idg") != null) {
            CmdIdleDeathGamble idgCmd = new CmdIdleDeathGamble(this);
            getCommand("idg").setExecutor(idgCmd);
            getCommand("idg").setTabCompleter(idgCmd);
        } else {
            getLogger().warning("Command /idg is missing from plugin.yml");
        }

        if (getCommand("seance") != null) {
            CmdSeance seanceCmd = new CmdSeance(this, seanceManager);
            getCommand("seance").setExecutor(seanceCmd);
            getCommand("seance").setTabCompleter(seanceCmd);
        } else {
            getLogger().warning("Command /seance is missing from plugin.yml");
        }

        if (getCommand("strawdoll") != null) {
            CmdStrawDoll sdCmd = new CmdStrawDoll(this, strawDollManager);
            getCommand("strawdoll").setExecutor(sdCmd);
            getCommand("strawdoll").setTabCompleter(sdCmd);
        } else {
            getLogger().warning("Command /strawdoll is missing from plugin.yml");
        }

        // Ten Shadows command
        if (getCommand("tenshadows") != null) {
            CmdTenShadows tsCmd = new CmdTenShadows(this, tenShadowsManager);
            getCommand("tenshadows").setExecutor(tsCmd);
            getCommand("tenshadows").setTabCompleter(tsCmd);
        } else {
            getLogger().warning("Command /tenshadows is missing from plugin.yml");
        }

        // Limitless command (full implementation)
        if (getCommand("limitless") != null) {
            CmdLimitless limitlessCmd = new CmdLimitless(this);
            getCommand("limitless").setExecutor(limitlessCmd);
            getCommand("limitless").setTabCompleter(limitlessCmd);
        } else {
            getLogger().warning("Command /limitless is missing from plugin.yml");
        }

        // Deadly Sentencing command
        if (getCommand("deadlysentencing") != null) {
            CmdDeadlySentencing dsCmd = new CmdDeadlySentencing(this);
            getCommand("deadlysentencing").setExecutor(dsCmd);
            getCommand("deadlysentencing").setTabCompleter(dsCmd);
        } else {
            getLogger().warning("Command /deadlysentencing is missing from plugin.yml");
        }

        // Blood Manipulation command
        if (getCommand("bloodmanip") != null) {
            CmdBloodManip bmCmd = new CmdBloodManip(this);
            getCommand("bloodmanip").setExecutor(bmCmd);
            getCommand("bloodmanip").setTabCompleter(bmCmd);
        } else {
            getLogger().warning("Command /bloodmanip is missing from plugin.yml");
        }

        // Six Eyes trait command
        if (getCommand("sixtrait") != null) {
            CmdSixTrait sixTraitCmd = new CmdSixTrait(this);
            getCommand("sixtrait").setExecutor(sixTraitCmd);
            getCommand("sixtrait").setTabCompleter(sixTraitCmd);
        } else {
            getLogger().warning("Command /sixtrait is missing from plugin.yml");
        }

        actionbarUI.start();
        bossbarUI.start();
        cursedEnergyManager.startRegenTask();

        rikaManager.start();
        copyLifecycleListener.start();
        projectionManager.start();
        tenShadowsManager.start();

        Bukkit.getOnlinePlayers().forEach(p -> {
            playerDataStore.load(p.getUniqueId());
            cursedEnergyManager.ensureInitialized(p.getUniqueId());
            bossbarUI.attachPlayer(p);
        });

        getLogger().info("JJKCursedTools enabled with Copy + Rika + Ten Shadows.");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            bossbarUI.detachPlayer(p);
            actionbarUI.clear(p.getUniqueId());
            playerDataStore.save(p.getUniqueId());
        });

        getLogger().info("JJKCursedTools disabled.");
    }

    public ConfigManager cfg() { return configManager; }
    public PlayerDataStore data() { return playerDataStore; }

    public TechniqueRegistry techniques() { return techniqueRegistry; }
    public TechniqueManager techniqueManager() { return techniqueManager; }

    public CursedEnergyManager ce() { return cursedEnergyManager; }
    public CooldownManager cooldowns() { return cooldownManager; }
    public RegenLockManager regenLock() { return regenLockManager; }
    public NullifyManager nullify() { return nullifyManager; }

    public ItemIds itemIds() { return itemIds; }
    public CursedToolFactory tools() { return cursedToolFactory; }

    public ActionbarUI actionbarUI() { return actionbarUI; }
    public BossbarUI bossbarUI() { return bossbarUI; }

    public AbilityService abilityService() { return abilityService; }
    public CommandRouter router() { return commandRouter; }

    public WheelTierManager wheelTierManager() { return wheelTierManager; }
    public WheelUI wheelUI() { return wheelUI; }

    public CreationManager creationManager() { return creationManager; }
    public CursedSpeechManager cursedSpeech() { return cursedSpeechManager; }
    public BoogieWoogieManager boogieWoogie() { return boogieWoogieManager; }
    public PlayfulCloudManager playfulCloud() { return playfulCloudManager; }

    public GlobalDataStore global() { return globalDataStore; }
    public CopyManager copy() { return copyManager; }
    public RikaManager rika() { return rikaManager; }
    public RikaStorageGUI rikaStorage() { return rikaStorageGUI; }
    public CursedBodyItem cursedBody() { return cursedBodyItem; }

    public DomainManager domainManager() { return domainManager; }
    public TechniqueRegistry techniqueRegistry() { return techniqueRegistry; }

    public IdleDeathGambleManager idgManager() { return idleDeathGambleManager; }

    public SeanceManager seanceManager() { return seanceManager; }

    public StrawDollManager strawDollManager() { return strawDollManager; }

    public ProjectionManager projectionManager() { return projectionManager; }
    public BetterModelBridge betterModelBridge() { return betterModelBridge; }

    public TenShadowsManager tenShadows() { return tenShadowsManager; }
    public ShadowStorageGUI shadowStorage() { return shadowStorageGUI; }

    public LimitlessManager limitless() { return limitlessManager; }
    public SixEyesTrait sixEyes() { return sixEyesTrait; }
    public DeadlySentencingManager deadlySentencing() { return deadlySentencingManager; }
    public BloodManipulationManager bloodManip() { return bloodManipulationManager; }

    public void reloadAll() {
        configManager.load();
        if (globalDataStore != null) globalDataStore.reload();
        Bukkit.getOnlinePlayers().forEach(p -> bossbarUI.attachPlayer(p));
    }

    private void registerTechniques() {
        techniqueRegistry.register(new GravityTechnique(this));
        techniqueRegistry.register(new CreationTechnique(this));
        techniqueRegistry.register(new CursedSpeechTechnique());
        techniqueRegistry.register(new BoogieWoogieTechnique());
        techniqueRegistry.register(new CopyTechnique(this));
        techniqueRegistry.register(new ProjectionTechnique(this));
        techniqueRegistry.register(new IdleDeathGambleTechnique(this));
        techniqueRegistry.register(new SeanceTechnique(this));
        techniqueRegistry.register(new StrawDollTechnique(this));
        techniqueRegistry.register(new TenShadowsTechnique(this));
        techniqueRegistry.register(new LimitlessTechnique(this));
        techniqueRegistry.register(new DeadlySentencingTechnique(this));
        techniqueRegistry.register(new JacobsLadderTechnique(this));
        techniqueRegistry.register(new CurseManipulationTechnique(this));
        techniqueRegistry.register(new BloodManipulationTechnique(this));
    }
}