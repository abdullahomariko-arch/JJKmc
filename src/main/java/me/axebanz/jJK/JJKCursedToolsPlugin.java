package me.axebanz.jJK;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Main plugin class for JJKmc.
 */
public class JJKCursedToolsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private TechniqueRegistry techniqueRegistry;
    private TechniqueManager techniqueManager;
    private CursedEnergyManager ceManager;
    private CooldownManager cooldownManager;
    private PlayerDataStore playerDataStore;
    private BossbarUI bossbarUI;
    private ActionbarUI actionbarUI;

    // Technique managers
    private ProjectionManager projectionManager;
    private ProjectionFreezeHandler projectionFreezeHandler;
    private CreationManager creationManager;
    private IdleDeathGambleManager idleDeathGambleManager;
    private BoogieWoogieManager boogieWoogieManager;
    private CursedSpeechManager cursedSpeechManager;
    private StrawDollManager strawDollManager;
    private CopyManager copyManager;
    private SeanceManager seanceManager;
    private RikaManager rikaManager;
    private DomainManager domainManagerInstance;
    private WheelTierManager wheelTierManager;
    private NullifyManager nullifyManager;
    private RegenLockManager regenLockManager;
    private AbilityService abilityService;
    private CursedToolFactory cursedToolFactory;
    private PlayfulCloudManager playfulCloudManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Core managers
        configManager = new ConfigManager(this);
        techniqueRegistry = new TechniqueRegistry();
        techniqueManager = new TechniqueManager(techniqueRegistry);
        ceManager = new CursedEnergyManager(configManager);
        cooldownManager = new CooldownManager();
        playerDataStore = new PlayerDataStore(this);
        bossbarUI = new BossbarUI(this);
        actionbarUI = new ActionbarUI(this);
        nullifyManager = new NullifyManager();
        regenLockManager = new RegenLockManager();
        abilityService = new AbilityService(this);
        cursedToolFactory = new CursedToolFactory();
        rikaManager = new RikaManager(this);
        playfulCloudManager = new PlayfulCloudManager(this);

        // Domain
        domainManagerInstance = new DomainManager(this);

        // Technique-specific managers
        projectionFreezeHandler = new ProjectionFreezeHandler(this);
        projectionManager = new ProjectionManager(this, projectionFreezeHandler, cooldownManager);
        creationManager = new CreationManager(this);
        idleDeathGambleManager = new IdleDeathGambleManager(this, cooldownManager, domainManagerInstance);
        boogieWoogieManager = new BoogieWoogieManager(this, cooldownManager);
        cursedSpeechManager = new CursedSpeechManager(this, cooldownManager);
        strawDollManager = new StrawDollManager(this, cooldownManager);
        copyManager = new CopyManager(this);
        seanceManager = new SeanceManager(this);
        wheelTierManager = new WheelTierManager(this);

        // Register techniques
        techniqueRegistry.register(new ProjectionTechnique(this, projectionManager));
        techniqueRegistry.register(new CreationTechnique(this, creationManager));
        techniqueRegistry.register(new IdleDeathGambleTechnique(this, idleDeathGambleManager));
        techniqueRegistry.register(new BoogieWoogieTechnique(this, boogieWoogieManager));
        techniqueRegistry.register(new CursedSpeechTechnique(this, cursedSpeechManager));
        techniqueRegistry.register(new StrawDollTechnique(this, strawDollManager));
        techniqueRegistry.register(new CopyTechnique(this, copyManager));
        techniqueRegistry.register(new SeanceTechnique(this, seanceManager));
        techniqueRegistry.register(new GravityTechnique(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new PlayerLifecycleListener(this, playerDataStore, ceManager, bossbarUI), this);
        getServer().getPluginManager().registerEvents(
                new ProjectionListener(this, projectionManager), this);
        getServer().getPluginManager().registerEvents(
                new DomainListener(this, domainManagerInstance), this);
        getServer().getPluginManager().registerEvents(
                new CopyListener(this, copyManager), this);
        getServer().getPluginManager().registerEvents(
                new CopyLifecycleListener(this, copyManager), this);
        getServer().getPluginManager().registerEvents(
                new CursedSpeechListener(this, cursedSpeechManager), this);
        getServer().getPluginManager().registerEvents(
                new IdleDeathGambleListener(this, idleDeathGambleManager), this);
        getServer().getPluginManager().registerEvents(
                new StrawDollListener(this, strawDollManager), this);
        getServer().getPluginManager().registerEvents(
                new WheelDamageListener(this, wheelTierManager), this);
        getServer().getPluginManager().registerEvents(
                new WheelCombatHandler(this), this);
        getServer().getPluginManager().registerEvents(
                new CombatListener(this, abilityService), this);
        getServer().getPluginManager().registerEvents(
                new ToolUseListener(this), this);

        SeanceListener seanceListener = new SeanceListener(this, seanceManager, playerDataStore);
        getServer().getPluginManager().registerEvents(seanceListener, this);

        // Register commands
        registerCommands();

        // CE regen task
        new BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                    if (!regenLockManager.isRegenLocked(p.getUniqueId())) {
                        ceManager.regen(p.getUniqueId());
                        bossbarUI.updateCeBar(p, ceManager.get(p.getUniqueId()), ceManager.getMax());
                    }
                }
            }
        }.runTaskTimer(this, 0L, configManager.ceRegenInterval());

        // Séance tick task
        new BukkitRunnable() {
            @Override
            public void run() {
                seanceManager.tickIncantations();
            }
        }.runTaskTimer(this, 0L, 1L);

        // Initialize API (api is the class me.axebanz.jJK.api — same package, no import needed)
        api.init(this);

        getLogger().info("JJKmc enabled successfully.");
    }

    private void registerCommands() {
        // commands is the class me.axebanz.jJK.commands — same package, no import needed.
        // /projection - Bug Fix #3 checks inside command
        commands.registerIfPresent(this, "projection",
                new CmdProjection(this, projectionManager),
                new CmdProjection(this, projectionManager));

        // /creation - Bug Fix #3
        commands.registerIfPresent(this, "creation",
                new CmdCreation(this, creationManager),
                new CmdCreation(this, creationManager));

        // /idledeathgamble - Bug Fix #3
        commands.registerIfPresent(this, "idledeathgamble",
                new CmdIdleDeathGamble(this, idleDeathGambleManager),
                new CmdIdleDeathGamble(this, idleDeathGambleManager));

        // /strawdoll - Bug Fix #8: new command
        if (getCommand("strawdoll") != null) {
            CmdStrawDoll sdCmd = new CmdStrawDoll(this, strawDollManager);
            getCommand("strawdoll").setExecutor(sdCmd);
            getCommand("strawdoll").setTabCompleter(sdCmd);
        } else {
            getLogger().warning("Command /strawdoll is missing from plugin.yml");
        }

        // /domain
        commands.registerIfPresent(this, "domain",
                new CmdDomain(this, domainManagerInstance),
                new CmdDomain(this, domainManagerInstance));

        // /seance
        commands.registerIfPresent(this, "seance",
                new CmdSeance(this, seanceManager),
                new CmdSeance(this, seanceManager));

        // /boogiewoogie
        commands.registerIfPresent(this, "boogiewoogie",
                new CmdBoogieWoogie(this, boogieWoogieManager),
                new CmdBoogieWoogie(this, boogieWoogieManager));

        // /cursedspeech
        commands.registerIfPresent(this, "cursedspeech",
                new CmdCursedSpeech(this, cursedSpeechManager),
                new CmdCursedSpeech(this, cursedSpeechManager));

        // /copy
        commands.registerIfPresent(this, "copy",
                new CmdCopy(this, copyManager),
                new CmdCopy(this, copyManager));

        // /technique
        commands.registerIfPresent(this, "technique",
                new CmdTechnique(this, techniqueRegistry, techniqueManager),
                new CmdTechnique(this, techniqueRegistry, techniqueManager));

        // /jjkgive - Bug Fix #10
        commands.registerIfPresent(this, "jjkgive",
                new CmdGive(this, cursedToolFactory),
                new CmdGive(this, cursedToolFactory));

        // /permadeath
        commands.registerIfPresent(this, "permadeath",
                new CmdPermadeath(this, playerDataStore),
                new CmdPermadeath(this, playerDataStore));

        // /setwaitingroom
        commands.registerIfPresent(this, "setwaitingroom",
                new CmdSetWaitingRoom(this),
                new CmdSetWaitingRoom(this));

        // /wheel
        commands.registerIfPresent(this, "wheel",
                new CmdWheel(this, wheelTierManager),
                new CmdWheel(this, wheelTierManager));

        // /status
        commands.registerIfPresent(this, "status",
                new CmdStatus(this, ceManager),
                new CmdStatus(this, ceManager));

        // /removebindingvow
        commands.registerIfPresent(this, "removebindingvow",
                new CmdRemoveBindingVow(this, strawDollManager),
                new CmdRemoveBindingVow(this, strawDollManager));

        // /jjk (help)
        commands.registerIfPresent(this, "jjk",
                new CmdHelp(this),
                new CmdHelp(this));
    }

    @Override
    public void onDisable() {
        bossbarUI.hideAll();
        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
            playerDataStore.unload(p.getUniqueId());
        }
        getLogger().info("JJKmc disabled.");
    }

    // Getters
    public ConfigManager cfg() { return configManager; }
    public TechniqueManager techniqueManager() { return techniqueManager; }
    public TechniqueRegistry techniqueRegistry() { return techniqueRegistry; }
    public CursedEnergyManager ceManager() { return ceManager; }
    public CooldownManager cooldownManager() { return cooldownManager; }
    public PlayerDataStore playerDataStore() { return playerDataStore; }
    public BossbarUI bossbarUI() { return bossbarUI; }
    public ProjectionManager projectionManager() { return projectionManager; }
    public DomainManager domainManager() { return domainManagerInstance; }
    public SeanceManager seanceManager() { return seanceManager; }
    public StrawDollManager strawDollManager() { return strawDollManager; }
    public NullifyManager nullifyManager() { return nullifyManager; }
    public RegenLockManager regenLockManager() { return regenLockManager; }
    public AbilityService abilityService() { return abilityService; }
}
