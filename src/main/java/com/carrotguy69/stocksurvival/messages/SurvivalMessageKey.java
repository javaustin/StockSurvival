package com.carrotguy69.stocksurvival.messages;

public enum SurvivalMessageKey {
    NO_ACCESS("errors.command.no-access"),
    COMMAND_PLAYER_ONLY("errors.command.player-only"),

    TARGET_PROTECTED("spawn-prot.target-protected"),
    ATTACKER_PROTECTED("spawn-prot.attacker-protected"),

    FORFEIT("commands.forfeit"),
    FORFEIT_CONFIRM("commands.forfeit-confirm"),
    UNPROTECTED("commands.forfeit-error-not-protected"),

    PROTECTION_VIEW("commands.protection-view"),

    END_DISABLED("end-disabled"),

    IN_COMBAT("combat-logger.in-combat"),
    OUT_COMBAT("combat-logger.out-of-combat"),
    COMBAT_LOGGED("combat-logger.combat-logged"),

    DEATH_LOCATION("death-location"),

    ON_JOIN_FIRST("on-join-first"),
    ON_JOIN("on-join"),
    ON_LEAVE("on-leave"),

    MISSING_GENERAL("errors.args.missing.general"),
    PLAYER_IS_SELF("errors.player.is-self"),
    PLAYER_NOT_FOUND("errors.player.not-found"),
    PLAYER_OUTRANKS_SENDER("errors.player.outranks-sender"),
    PLAYER_IS_OFFLINE("errors.player.is-offline"),
    ;

    private final String path;

    SurvivalMessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
