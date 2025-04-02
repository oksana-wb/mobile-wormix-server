package com.pragmatix.pvp.services.battletracking;

import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.services.battletracking.handlers.HandlerI;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Конечный автомат отслеживания состояний боя
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.06.11 18:03
 * @see PvpBattleStateEnum
 */
@Component
public class BattleStateTrackerFactory implements ApplicationContextAware {

    // интервал отрравки PvpActionEx на клиенте 1сек.
    public static final int COMMANDS_INTERVAL = 1000;

    private final String[][] PvE_FRIEND_RULES = new String[][]{
            {"                          ", "  IdleTimeout          ", "  StateTimeout   ", "  AllInState     ", " WidenSearch  ", "            PvpAction   ", "  EndTurn  ", "  EndBattle  ", "CancelBattle", "  Disconnect  ", "  Unbind  ", "  Desync  "},
            {"WaitProfiles (WP)         ", "                       ", "End/Cancel       ", "WFJ/Call         ", "              ", "                        ", "           ", "             ", "End/Cancel  ", "End/Cancel    ", "          ", "End/Cancel"},
            {"WaitFriendsJoin (WFJ)     ", "                       ", "                 ", "WBR/Create       ", "              ", "                        ", "           ", "             ", "End/Cancel  ", "End/Cancel    ", "          ", "End/Cancel"},
            {"WaitBattleReady (WBR)     ", "                       ", "End/Cancel       ", "R/Start          ", "              ", "                        ", "           ", "             ", "End/Cancel  ", "End/Cancel    ", "          ", "End/Cancel"},
            {"EnvironmentInTurn (ET)    ", "ET/EnvWarnTimeout      ", "?/TransferTurnEnv", "?/TransferTurnEnv", "              ", "                        ", "           ", "WEC/EndBattle", "            ", "ET/Disconnect ", "ET/UnWTT  ", "Desync    "},
            {"ReadyToDispatch (R)       ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy        ", "              ", "          R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "            ", "R/Disconnect  ", "Syn/Proxy ", "Syn/Proxy "},
            {"WaitForReplayCommand (WR) ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy        ", "              ", "          R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "            ", "WR/Disconnect ", "Syn/Proxy ", "Syn/Proxy "},
            {"Synchronized (Syn)        ", "WR/RequestRetryCommand ", "WTT/ForcedEndTurn", "Desync           ", "              ", "          R/ValidCommand", "WTT/EndTurn", "WEC/EndBattle", "            ", "Syn/Disconnect", "Syn/Unbind", "Desync    "},
            {"WaitForTurnTransfer (WTT) ", "WTT/DispatchLastCommand", "?/TransferTurnEnv", "?/TransferTurnEnv", "              ", "          WTT           ", "WTT        ", "WEC/EndBattle", "            ", "WTT/Disconnect", "WTT/UnWTT ", "Desync    "},
            {"WaitEndBattleConfirm (WEC)", "WEC/RetryEndBattleReq  ", "End/Finalize     ", "End/Finalize     ", "              ", "                        ", "           ", "             ", "            ", "              ", "          ", "          "},
            {"EndBattle (End)           ", "End                    ", "End              ", "End              ", "End           ", "          End           ", "End        ", "End          ", "End/Rollback", "End           ", "End       ", "End       "},
    };

    private final String[][] FRIEND_PvP_RULES = new String[][]{
            {"                          ", "  IdleTimeout          ", "  StateTimeout   ", "  AllInState  ", " WidenSearch  ", "               PvpAction   ", "  EndTurn  ", "  EndBattle  ", "CancelBattle", "  Disconnect  ", "  Unbind  ", "  Desync  "},
            {"WaitProfiles (WP)         ", "                       ", "End/Cancel       ", "WFJ/Call      ", "              ", "                           ", "           ", "             ", "End/Cancel  ", "End/Cancel    ", "          ", "End/Cancel"},
            {"WaitFriendsJoin (WFJ)     ", "                       ", "                 ", "WBR/Create    ", "              ", "                           ", "           ", "             ", "End/Cancel  ", "End/Cancel    ", "          ", "End/Cancel"},
            {"WaitBattleReady (WBR)     ", "                       ", "End/Cancel       ", "R/Start       ", "              ", "                           ", "           ", "             ", "End/Cancel  ", "End/Cancel    ", "          ", "End/Cancel"},
            {"ReadyToDispatch (R)       ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy     ", "              ", "             R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "            ", "R/Disconnect  ", "Syn/Proxy ", "Syn/Proxy "},
            {"WaitForReplayCommand (WR) ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy     ", "              ", "             R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "            ", "WR/Disconnect ", "Syn/Proxy ", "Syn/Proxy "},
            {"Synchronized (Syn)        ", "WR/RequestRetryCommand ", "WTT/ForcedEndTurn", "Desync        ", "              ", "             R/ValidCommand", "WTT/EndTurn", "WEC/EndBattle", "            ", "Syn/Disconnect", "Syn/Unbind", "Desync    "},
            {"WaitForTurnTransfer (WTT) ", "WTT/DispatchLastCommand", "R/TransferTurn   ", "R/TransferTurn", "              ", "             WTT           ", "WTT        ", "WEC/EndBattle", "            ", "WTT/Disconnect", "WTT/UnWTT ", "Desync    "},
            {"WaitEndBattleConfirm (WEC)", "WEC/RetryEndBattleReq  ", "End/Finalize     ", "End/Finalize  ", "              ", "                           ", "           ", "             ", "            ", "              ", "          ", "          "},
            {"EndBattle (End)           ", "End                    ", "End              ", "End           ", "End           ", "             End           ", "End        ", "End          ", "End/Rollback", "End           ", "End       ", "End       "},
    };

    private final String[][] WAGER_PvP_FRIEND_RULES = new String[][]{
            {"                          ", "  IdleTimeout          ", "  StateTimeout   ", "  AllInState  ", " WidenSearch  ", "  Match   ", "  PvpAction   ", "  EndTurn  ", "  EndBattle  ", " CancelBattle ", "  Disconnect  ", "  Unbind  ", "  Desync      "},
            {"WaitProfiles (WP)         ", "                       ", "End/Cancel       ", "WFJ/Call      ", "              ", "          ", "              ", "           ", "             ", "End/Cancel    ", "End/Cancel    ", "          ", "End/Cancel    "},
            {"WaitFriendsJoin (WFJ)     ", "                       ", "                 ", "ML/Matchmake  ", "              ", "          ", "              ", "           ", "             ", "End/Cancel    ", "End/Cancel    ", "          ", "End/Cancel    "},
            {"MatchmakingLobby (ML)     ", "                       ", "                 ", "              ", "ML/WidenSearch", "WBR/Create", "              ", "           ", "             ", "End/LeaveLobby", "End/LeaveLobby", "          ", "End/LeaveLobby"},
            {"WaitBattleReady (WBR)     ", "                       ", "End/Cancel       ", "R/Start       ", "              ", "          ", "              ", "           ", "             ", "End/Cancel    ", "End/Cancel    ", "          ", "End/Cancel    "},
            {"ReadyToDispatch (R)       ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy     ", "              ", "          ", "R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "              ", "R/Disconnect  ", "Syn/Proxy ", "Syn/Proxy     "},
            {"WaitForReplayCommand (WR) ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy     ", "              ", "          ", "R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "              ", "WR/Disconnect ", "Syn/Proxy ", "Syn/Proxy     "},
            {"Synchronized (Syn)        ", "WR/RequestRetryCommand ", "WTT/ForcedEndTurn", "Desync        ", "              ", "          ", "R/ValidCommand", "WTT/EndTurn", "WEC/EndBattle", "              ", "Syn/Disconnect", "Syn/Unbind", "Desync        "},
            {"WaitForTurnTransfer (WTT) ", "WTT/DispatchLastCommand", "R/TransferTurn   ", "R/TransferTurn", "              ", "          ", "WTT           ", "WTT        ", "WEC/EndBattle", "              ", "WTT/Disconnect", "WTT/UnWTT ", "Desync        "},
            {"WaitEndBattleConfirm (WEC)", "WEC/RetryEndBattleReq  ", "End/Finalize     ", "End/Finalize  ", "              ", "          ", "              ", "           ", "             ", "              ", "              ", "          ", "              "},
            {"EndBattle (End)           ", "End                    ", "End              ", "End           ", "End           ", "End       ", "End           ", "End        ", "End          ", "End/Rollback  ", "End           ", "End       ", "End           "},
            {"DropBattle (Drop)         ", "                       ", "                 ", "              ", "              ", "          ", "              ", "           ", "             ", "              ", "              ", "          ", "              "},
    };

    private final String[][] WAGER_PvP_RULES = new String[][]{
            {"                          ", "  IdleTimeout          ", "  StateTimeout   ", "  AllInState  ", " WidenSearch  ", "  Match   ", "  PvpAction   ", "  EndTurn  ", "  EndBattle  ", " CancelBattle ", "  Disconnect  ", "  Unbind  ", "  Desync      "},
            {"WaitProfiles (WP)         ", "                       ", "End/Cancel       ", "ML/Matchmake  ", "              ", "          ", "              ", "           ", "             ", "End/Cancel    ", "End/Cancel    ", "          ", "End/Cancel    "},
            {"MatchmakingLobby (ML)     ", "                       ", "                 ", "              ", "ML/WidenSearch", "WBR/Create", "              ", "           ", "             ", "End/LeaveLobby", "End/LeaveLobby", "          ", "End/LeaveLobby"},
            {"WaitBattleReady (WBR)     ", "                       ", "End/Cancel       ", "R/Start       ", "              ", "          ", "              ", "           ", "             ", "End/Cancel    ", "End/Cancel    ", "          ", "End/Cancel    "},
            {"ReadyToDispatch (R)       ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy     ", "              ", "          ", "R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "              ", "R/Disconnect  ", "Syn/Proxy ", "Syn/Proxy     "},
            {"WaitForReplayCommand (WR) ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy     ", "              ", "          ", "R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "              ", "WR/Disconnect ", "Syn/Proxy ", "Syn/Proxy     "},
            {"Synchronized (Syn)        ", "WR/RequestRetryCommand ", "WTT/ForcedEndTurn", "Desync        ", "              ", "          ", "R/ValidCommand", "WTT/EndTurn", "WEC/EndBattle", "              ", "Syn/Disconnect", "Syn/Unbind", "Desync        "},
            {"WaitForTurnTransfer (WTT) ", "WTT/DispatchLastCommand", "R/TransferTurn   ", "R/TransferTurn", "              ", "          ", "WTT           ", "WTT        ", "WEC/EndBattle", "              ", "WTT/Disconnect", "WTT/UnWTT ", "Desync        "},
            {"WaitEndBattleConfirm (WEC)", "WEC/RetryEndBattleReq  ", "End/Finalize     ", "End/Finalize  ", "              ", "          ", "              ", "           ", "             ", "              ", "              ", "          ", "              "},
            {"EndBattle (End)           ", "End                    ", "End              ", "End           ", "End           ", "End       ", "End           ", "End        ", "End          ", "End/Rollback  ", "End           ", "End       ", "End           "},
            {"DropBattle (Drop)         ", "                       ", "                 ", "              ", "              ", "          ", "              ", "           ", "             ", "              ", "              ", "          ", "              "},
    };


    private final String[][] PvE_PARTNER_RULES = new String[][]{
            {"                          ", "  IdleTimeout          ", "  StateTimeout   ", "  AllInState     ",  " WidenSearch  ", "  Match   ","  PvpAction   ", "  EndTurn  ", "  EndBattle  ", "CancelBattle  ", "  Disconnect  ", "  Unbind  ", "  Desync      "},
            {"WaitProfiles (WP)         ", "                       ", "End/Cancel       ", "ML/Matchmake     ",  "              ", "          ","              ", "           ", "             ", "End/Cancel    ", "End/Cancel    ", "          ", "End/Cancel    "},
            {"MatchmakingLobby (ML)     ", "                       ", "                 ", "                 ",  "ML/WidenSearch", "WBR/Create","WBR/Create"   ,  "           ", "             ", "End/LeaveLobby", "End/LeaveLobby", "          ", "End/LeaveLobby"},
            {"WaitBattleReady (WBR)     ", "                       ", "End/Cancel       ", "R/Start          ",  "              ", "          ","              ", "           ", "             ", "End/Cancel    ", "End/Cancel    ", "          ", "End/Cancel    "},
            {"EnvironmentInTurn (ET)    ", "ET/EnvWarnTimeout      ", "?/TransferTurnEnv", "?/TransferTurnEnv",  "              ", "          ","              ", "           ", "WEC/EndBattle", "              ", "ET/Disconnect ", "ET/UnWTT  ", "Desync        "},
            {"ReadyToDispatch (R)       ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy        ",  "              ", "          ","R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "              ", "R/Disconnect  ", "Syn/Proxy ", "Syn/Proxy     "},
            {"WaitForReplayCommand (WR) ", "Syn/Proxy              ", "Syn/Proxy        ", "Syn/Proxy        ",  "              ", "          ","R/ValidCommand", "Syn/Proxy  ", "Syn/Proxy    ", "              ", "WR/Disconnect ", "Syn/Proxy ", "Syn/Proxy     "},
            {"Synchronized (Syn)        ", "WR/RequestRetryCommand ", "WTT/ForcedEndTurn", "Desync           ",  "              ", "          ","R/ValidCommand", "WTT/EndTurn", "WEC/EndBattle", "              ", "Syn/Disconnect", "Syn/Unbind", "Desync        "},
            {"WaitForTurnTransfer (WTT) ", "WTT/DispatchLastCommand", "?/TransferTurnEnv", "?/TransferTurnEnv",  "              ", "          ","WTT           ", "WTT        ", "WEC/EndBattle", "              ", "WTT/Disconnect", "WTT/UnWTT ", "Desync        "},
            {"WaitEndBattleConfirm (WEC)", "WEC/RetryEndBattleReq  ", "End/Finalize     ", "End/Finalize     ",  "              ", "          ","              ", "           ", "             ", "              ", "              ", "          ", "              "},
            {"EndBattle (End)           ", "End                    ", "End              ", "End              ",  "End           ", "End       ","End           ", "End        ", "End          ", "End/Rollback  ", "End           ", "End       ", "End           "},
    };

    private Map<PvpBattleType, BattleStateTrackerI> battleTrackers = new EnumMap<>(PvpBattleType.class);
    // от этого типа боя отказались, но набор правил необходимо задействовать. Учитывается заявка вместо этого
    private BattleStateTracker WAGER_PvP_2x2_FRIENDS_battleTracker;

    @Resource
    private CommandTranslator commandTranslator;
    public static final String HANDLERS_PACKAGE = "com.pragmatix.pvp.services.battletracking.handlers";

    @PostConstruct
    public void init() {
        battleTrackers.put(PvpBattleType.FRIEND_PvP, new BattleStateTracker(constructStateMachineRules(FRIEND_PvP_RULES), commandTranslator, PvpBattleStateEnum.WaitProfiles));

        battleTrackers.put(PvpBattleType.PvE_FRIEND, new BattleStateTracker(constructStateMachineRules(PvE_FRIEND_RULES), commandTranslator, PvpBattleStateEnum.WaitProfiles));
        battleTrackers.put(PvpBattleType.PvE_PARTNER, new BattleStateTracker(constructStateMachineRules(PvE_PARTNER_RULES), commandTranslator, PvpBattleStateEnum.WaitProfiles));

        battleTrackers.put(PvpBattleType.WAGER_PvP_DUEL, new BattleStateTracker(constructStateMachineRules(WAGER_PvP_RULES), commandTranslator, PvpBattleStateEnum.WaitProfiles));
        battleTrackers.put(PvpBattleType.WAGER_PvP_3_FOR_ALL, new BattleStateTracker(constructStateMachineRules(WAGER_PvP_RULES), commandTranslator, PvpBattleStateEnum.WaitProfiles));
        battleTrackers.put(PvpBattleType.WAGER_PvP_2x2, new BattleStateTracker(constructStateMachineRules(WAGER_PvP_RULES), commandTranslator, PvpBattleStateEnum.WaitProfiles));

        WAGER_PvP_2x2_FRIENDS_battleTracker =  new BattleStateTracker(constructStateMachineRules(WAGER_PvP_FRIEND_RULES), commandTranslator, PvpBattleStateEnum.WaitProfiles);
    }

    public BattleStateTrackerI getBattleStateTracker(PvpBattleType battleType, BattleWager battleWager) {
        return battleWager == BattleWager.WAGER_50_2x2_FRIENDS ? WAGER_PvP_2x2_FRIENDS_battleTracker :  battleTrackers.get(battleType);
    }

    public Map<PvpBattleStateEnum, Map<PvpBattleActionEnum, CellItem>> constructStateMachineRules(String[][] rules) {
        Map<PvpBattleStateEnum, Map<PvpBattleActionEnum, CellItem>> stateMachineRules = new HashMap<PvpBattleStateEnum, Map<PvpBattleActionEnum, CellItem>>();

        String[] actions = rules[0];
        PvpBattleActionEnum[] actionsEnum = new PvpBattleActionEnum[actions.length];
        for(int i = 1; i < actions.length; i++) {
            String action = actions[i];
            actionsEnum[i] = PvpBattleActionEnum.valueOf(action.trim());
        }

        for(int i = 1; i < rules.length; i++) {
            String[] stateRules = rules[i];

            Map<PvpBattleActionEnum, CellItem> map = new HashMap<PvpBattleActionEnum, CellItem>();
            PvpBattleStateEnum currentState = PvpBattleStateEnum.valueOf(stateRules[0].split(" ")[0]);
            stateMachineRules.put(currentState, map);


            for(int j = 1; j < stateRules.length; j++) {
                CellItem cellItem = createCellItem(stateRules[j], currentState);
                map.put(actionsEnum[j], cellItem);
            }
        }

        return stateMachineRules;
    }

    private CellItem createCellItem(String cell, PvpBattleStateEnum currentState) {
        cell = cell.trim();
        if(cell.isEmpty()) {
            return new CellItem(currentState, getHandlerInstance(HANDLERS_PACKAGE + ".LazyHandler"));
        } else if(cell.equals("Desync")) {
            return new CellItem(PvpBattleStateEnum.EndBattle, getHandlerInstance(HANDLERS_PACKAGE + ".DesyncBattleHandler"));
        } else {
            if(cell.contains("/")) {
                PvpBattleStateEnum state = getBattleStateByAlias(cell.split("/")[0]);
                HandlerI handler = getHandlerInstance(HANDLERS_PACKAGE + "." + cell.split("/")[1] + "Handler");
                return new CellItem(state, handler);
            } else {
                PvpBattleStateEnum state = getBattleStateByAlias(cell);
                HandlerI handler = getHandlerInstance(HANDLERS_PACKAGE + ".LazyHandler");
                return new CellItem(state, handler);

            }
        }
    }

    protected HandlerI getHandlerInstance(String className) {
        return applicationContext.getBean(className, HandlerI.class);
    }

    private PvpBattleStateEnum getBattleStateByAlias(String alias) {
        // след. состояние будет установлено в обработчике
        if(alias.equals("?")) {
            return null;
        }
        for(PvpBattleStateEnum battleStateEnum : PvpBattleStateEnum.values()) {
            if(battleStateEnum.alias.equals(alias)) {
                return battleStateEnum;
            }
        }
        throw new IllegalArgumentException("BattleStateEnum not found by alias [" + alias + "]!");
    }

    public static class CellItem {

        public PvpBattleStateEnum state;
        public HandlerI handler;


        public CellItem(PvpBattleStateEnum state, HandlerI handler) {
            this.state = state;
            this.handler = handler;
        }

    }

    private ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
