-- Oracle Outbox Table Setup with Advanced Queuing (AQ)
-- This script creates the necessary Oracle objects for the transactional outbox pattern

-- ============================================================================
-- 1. Create Sequence for Outbox Message IDs
-- ============================================================================
CREATE SEQUENCE OUTBOX_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ============================================================================
-- 2. Create Outbox Messages Table
-- ============================================================================
CREATE TABLE OUTBOX_MESSAGES (
    ID NUMBER(19) PRIMARY KEY,
    PAYLOAD CLOB NOT NULL,
    MESSAGE_KEY VARCHAR2(500) NOT NULL,
    TOPIC VARCHAR2(255) NOT NULL,
    PUBLISHED NUMBER(1) DEFAULT 0 NOT NULL,
    CREATED_AT TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PUBLISHED_AT TIMESTAMP(6) WITH TIME ZONE,
    CLIENT_ID VARCHAR2(255),
    TASK_ID VARCHAR2(255),
    CONSTRAINT CHK_PUBLISHED CHECK (PUBLISHED IN (0, 1))
);

-- ============================================================================
-- 3. Create Indexes for Performance
-- ============================================================================
CREATE INDEX IDX_OUTBOX_PUBLISHED ON OUTBOX_MESSAGES(PUBLISHED, CREATED_AT);
CREATE INDEX IDX_OUTBOX_MSG_KEY ON OUTBOX_MESSAGES(MESSAGE_KEY);
CREATE INDEX IDX_OUTBOX_TASK_ID ON OUTBOX_MESSAGES(TASK_ID, PUBLISHED, CREATED_AT);

-- ============================================================================
-- 4. Create Oracle AQ Queue Table (Optional - for AQ/JMS integration)
-- ============================================================================
-- This creates a queue table for Oracle Advanced Queuing
-- Use this if you want to leverage Oracle AQ for guaranteed message delivery

BEGIN
    DBMS_AQADM.CREATE_QUEUE_TABLE(
        queue_table        => 'OUTBOX_QUEUE_TABLE',
        queue_payload_type => 'SYS.AQ$_JMS_TEXT_MESSAGE',
        comment            => 'Queue table for outbox messages'
    );
END;
/

-- ============================================================================
-- 5. Create Oracle AQ Queue
-- ============================================================================
BEGIN
    DBMS_AQADM.CREATE_QUEUE(
        queue_name  => 'OUTBOX_QUEUE',
        queue_table => 'OUTBOX_QUEUE_TABLE',
        comment     => 'Queue for outbox message processing'
    );
END;
/

-- ============================================================================
-- 6. Start the Queue
-- ============================================================================
BEGIN
    DBMS_AQADM.START_QUEUE(
        queue_name => 'OUTBOX_QUEUE'
    );
END;
/

-- ============================================================================
-- 7. Grant Permissions (adjust user as needed)
-- ============================================================================
-- Replace 'YOUR_APP_USER' with the actual database user
-- GRANT EXECUTE ON DBMS_AQ TO YOUR_APP_USER;
-- GRANT EXECUTE ON DBMS_AQADM TO YOUR_APP_USER;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON OUTBOX_MESSAGES TO YOUR_APP_USER;
-- GRANT SELECT ON OUTBOX_SEQ TO YOUR_APP_USER;

-- ============================================================================
-- 8. Optional: Create Trigger to Auto-populate ID
-- ============================================================================
CREATE OR REPLACE TRIGGER OUTBOX_MESSAGES_TRG
BEFORE INSERT ON OUTBOX_MESSAGES
FOR EACH ROW
WHEN (NEW.ID IS NULL)
BEGIN
    SELECT OUTBOX_SEQ.NEXTVAL INTO :NEW.ID FROM DUAL;
END;
/

-- ============================================================================
-- 9. Optional: Cleanup/Archival Job
-- ============================================================================
-- Create a scheduled job to archive old published messages (older than 30 days)
-- This prevents the outbox table from growing indefinitely

/*
BEGIN
    DBMS_SCHEDULER.CREATE_JOB(
        job_name        => 'OUTBOX_CLEANUP_JOB',
        job_type        => 'PLSQL_BLOCK',
        job_action      => 'BEGIN
                              DELETE FROM OUTBOX_MESSAGES
                              WHERE PUBLISHED = 1
                              AND PUBLISHED_AT < SYSTIMESTAMP - INTERVAL ''30'' DAY;
                              COMMIT;
                            END;',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=DAILY; BYHOUR=2',
        enabled         => TRUE,
        comments        => 'Clean up old published outbox messages daily at 2 AM'
    );
END;
/
*/

-- ============================================================================
-- 10. Verification Queries
-- ============================================================================
-- Check if objects were created successfully

-- Verify table
SELECT table_name FROM user_tables WHERE table_name = 'OUTBOX_MESSAGES';

-- Verify sequence
SELECT sequence_name FROM user_sequences WHERE sequence_name = 'OUTBOX_SEQ';

-- Verify indexes
SELECT index_name FROM user_indexes WHERE table_name = 'OUTBOX_MESSAGES';

-- Verify queue table
SELECT queue_table FROM user_queue_tables WHERE queue_table = 'OUTBOX_QUEUE_TABLE';

-- Verify queue
SELECT name, queue_table FROM user_queues WHERE name = 'OUTBOX_QUEUE';

-- ============================================================================
-- USAGE NOTES:
-- ============================================================================
-- 1. The OUTBOX_MESSAGES table stores messages to be published to Kafka
-- 2. The application polls this table and publishes unpublished messages
-- 3. Oracle AQ (OUTBOX_QUEUE) can be used for JMS-based integration if needed
-- 4. The cleanup job should be enabled in production to prevent table bloat
-- 5. Adjust the retention period (30 days) based on your requirements
-- ============================================================================
