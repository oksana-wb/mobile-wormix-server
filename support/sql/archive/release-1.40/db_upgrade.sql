-- обновить BattleAwardSettings.heroicMissionLevels

delete from wormswar.store WHERE key = 'BattleAwardSettings.heroicMissionLevels';
delete from wormswar.store WHERE key = 'HeroicMissionLevel.Level_0';
delete from wormswar.store WHERE key = 'HeroicMissionLevel.Level_1';
delete from wormswar.store WHERE key = 'HeroicMissionLevel.Level_2';
delete from wormswar.store WHERE key = 'HeroicMissionLevel.Level_3';