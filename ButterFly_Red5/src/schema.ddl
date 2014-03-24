
    create table butterflydb.gcm_user_mails (
        id integer not null auto_increment unique,
        mail varchar(45) unique,
        user_id integer,
        primary key (id),
        unique (mail)
    ) ENGINE=InnoDB;

    create table butterflydb.gcm_users (
        id integer not null auto_increment unique,
        primary key (id)
    ) ENGINE=InnoDB;

    create table butterflydb.reg_ids (
        id integer not null auto_increment unique,
        gcm_reg_id varchar(255) not null,
        user_id integer,
        primary key (id)
    ) ENGINE=InnoDB;

    create table butterflydb.stream_viewers (
        id integer not null auto_increment unique,
        userId integer,
        streamId integer,
        primary key (id)
    ) ENGINE=InnoDB;

    create table butterflydb.streams (
        id integer not null auto_increment unique,
        altitude double precision,
        broadcasterMail varchar(255) not null,
        isLive boolean,
        isPublic boolean,
        latitude double precision,
        longitude double precision,
        registerTime datetime not null,
        streamName varchar(255) not null,
        streamUrl varchar(255) not null,
        primary key (id)
    ) ENGINE=InnoDB;

    alter table butterflydb.gcm_user_mails 
        add index FK175F1E16F21DFF8 (user_id), 
        add constraint FK175F1E16F21DFF8 
        foreign key (user_id) 
        references butterflydb.gcm_users (id);

    alter table butterflydb.reg_ids 
        add index FK40B8148DF21DFF8 (user_id), 
        add constraint FK40B8148DF21DFF8 
        foreign key (user_id) 
        references butterflydb.gcm_users (id);

    alter table butterflydb.stream_viewers 
        add index FKDF086E2563426DC (streamId), 
        add constraint FKDF086E2563426DC 
        foreign key (streamId) 
        references butterflydb.streams (id);

    alter table butterflydb.stream_viewers 
        add index FKDF086E2E612240F (userId), 
        add constraint FKDF086E2E612240F 
        foreign key (userId) 
        references butterflydb.gcm_users (id);
