
CREATE TABLE IF NOT EXISTS Subscriber (
  id CHAR(64) primary key NOT NULL,
  open_id CHAR(128),
  source CHAR(64),
  receiver CHAR(128),
  nickname CHAR(64),
  create_at INTEGER,
  update_at INTEGER
);

CREATE TABLE IF NOT EXISTS Subscription (
  id CHAR(64) primary key NOT NULL,
  subscriber_id CHAR(64) NOT NULL,
  voyage_id CHAR(64) NOT NULL,
  infos CHAR(128),

  create_at INTEGER,
  notify_at INTEGER
);

CREATE TABLE IF NOT EXISTS Vessel (
  id CHAR(64) primary key NOT NULL,
  ship_code CHAR(64),
  ship_name CHAR(128),
  ship_cn_name CHAR(128),
  company CHAR(64),
  un_code CHAR(64),
  in_agent CHAR(64),
  out_agent CHAR(64),
  wharf_id CHAR(64),
  create_at INTEGER
);

CREATE TABLE IF NOT EXISTS Voyage (
  id CHAR(64) primary key NOT NULL,
  terminal_code CHAR(32),
  in_voy CHAR(64),
  out_voy CHAR(64),
  service_id CHAR(64),

  rcv_start CHAR(64),
  rcv_end CHAR(64),

  eta CHAR(64),
  pob CHAR(64),
  etb CHAR(64),
  etd CHAR(64),
  ata CHAR(64),
  atd CHAR(64),
  notes CHAR(128),

  vessel_id CHAR(64),
  create_at INTEGER, 
  update_at INTEGER
);

CREATE TABLE IF NOT EXISTS Wharf(
  id CHAR(64) primary key NOT NULL,
  name  CHAR(128),
  code  CHAR(64),
  website CHAR(64),
  period INTEGER,
  work_start INTEGER,
  work_end INTEGER,
  create_at INTEGER
);