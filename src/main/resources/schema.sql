CREATE TABLE BOT_USERS (
	USER_ID VARCHAR(36) PRIMARY KEY NOT NULL,
	USER_NAME VARCHAR(100) NOT NULL,
	LEVEL VARCHAR(100) NOT NULL
);

CREATE TABLE SENT_MESSAGES (
	MATCH_ID VARCHAR(36) NOT NULL,
	EVENT_ID VARCHAR(36) NOT NULL,
	CHAT_ID VARCHAR(36) NOT NULL,
	MESSAGE_ID VARCHAR(36) NOT NULL,
  constraint SENT_MESSAGES_PK primary key (MATCH_ID, EVENT_ID, CHAT_ID)
);