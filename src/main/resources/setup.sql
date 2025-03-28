CREATE TABLE IF NOT EXISTS bridgeffa_players
(
    uuid                   varchar(36) PRIMARY KEY,
    coins                  integer  DEFAULT 0,
    blocks_unlocked        longtext DEFAULT '{}',
    block_selected         longtext DEFAULT 'AIR',
    deathmessages_unlocked longtext DEFAULT '[]',
    deathmessage_selected  int DEFAULT 1
);