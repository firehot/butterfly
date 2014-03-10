
    create table butterflydb.gcm_user_mails (
        id integer not null auto_increment unique,
        mail varchar(45),
        user_id integer,
        primary key (id)
    );

    create table butterflydb.gcm_users (
        id integer not null auto_increment unique,
        primary key (id)
    );

    create table butterflydb.reg_ids (
        id integer not null auto_increment unique,
        gcm_reg_id varchar(255) not null,
        user_id integer,
        primary key (id)
    );

    create table butterflydb.streams (
        id integer not null auto_increment,
        altitude double precision,
        broadcasterMail varchar(255) not null,
        latitude double precision,
        isLive bit,
        longitude double precision,
        isPublic bit,
        registerTime datetime not null,
        streamName varchar(255) not null,
        streamUrl varchar(255) not null,
        primary key (id)
    );

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
