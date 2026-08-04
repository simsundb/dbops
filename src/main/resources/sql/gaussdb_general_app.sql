CREATE TABLE general_app_form (
    FILE_NAME VARCHAR(256) PRIMARY KEY,
    FILE_CONTENT TEXT,
    READ_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PARSED_FLAG VARCHAR(8) DEFAULT '0', 
    PARSE_TIME TIMESTAMP
);

COMMENT ON TABLE general_app_form IS '原始SQL文件表';
COMMENT ON COLUMN general_app_form.FILE_NAME IS '文件名唯一主键';
COMMENT ON COLUMN general_app_form.FILE_CONTENT IS '文件原始内容';
COMMENT ON COLUMN general_app_form.READ_TIME IS '读取入库时间';
COMMENT ON COLUMN general_app_form.PARSED_FLAG IS '是否已解析：0=未解析，1=已解析';
COMMENT ON COLUMN general_app_form.PARSE_TIME IS '解析完成时间';


CREATE TABLE general_app_form_parsed (
    FILE_NAME VARCHAR(256),
    SEQ_ID INTEGER,
    DDL_SQL TEXT,
    EXEC_FLAG VARCHAR(20),  
    EXEC_TIME TIMESTAMP,
    EXEC_MSG VARCHAR(4000),
    CONSTRAINT pk_parsed PRIMARY KEY (FILE_NAME, SEQ_ID)
);

COMMENT ON TABLE general_app_form_parsed IS '解析后的单条SQL表';
COMMENT ON COLUMN general_app_form_parsed.FILE_NAME IS '关联原始文件名';
COMMENT ON COLUMN general_app_form_parsed.SEQ_ID IS 'SQL序号联合主键';
COMMENT ON COLUMN general_app_form_parsed.DDL_SQL IS '单条可执行SQL不含分号';
COMMENT ON COLUMN general_app_form_parsed.EXEC_FLAG IS '执行状态:NULL/RUNNING/SUCCESS/FAILED';
COMMENT ON COLUMN general_app_form_parsed.EXEC_TIME IS '执行时间';
COMMENT ON COLUMN general_app_form_parsed.EXEC_MSG IS '执行结果或错误信息';


CREATE INDEX idx_form_parsed_flag ON general_app_form(PARSED_FLAG);
CREATE INDEX idx_parsed_exec_flag ON general_app_form_parsed(EXEC_FLAG);