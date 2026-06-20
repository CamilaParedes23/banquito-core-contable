-- R9-F: índices no únicos para consultas paginadas y trazabilidad de asientos.
-- No se modifica la semántica de correlación ni la idempotencia contable.

DELIMITER $$

DROP PROCEDURE IF EXISTS align_journal_entry_query_indexes$$
CREATE PROCEDURE align_journal_entry_query_indexes()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'ASIENTO_CONTABLE'
           AND INDEX_NAME = 'IDX_ASIENTO_FECHA_REGISTRO'
    ) THEN
        CREATE INDEX IDX_ASIENTO_FECHA_REGISTRO
            ON ASIENTO_CONTABLE (FECHA_CONTABLE, TIMESTAMP_REGISTRO, ID);
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'ASIENTO_CONTABLE'
           AND INDEX_NAME = 'IDX_ASIENTO_TRANSACCION'
    ) THEN
        CREATE INDEX IDX_ASIENTO_TRANSACCION
            ON ASIENTO_CONTABLE (TRANSACCION_UUID);
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'ASIENTO_CONTABLE'
           AND INDEX_NAME = 'IDX_ASIENTO_CORRELACION'
    ) THEN
        CREATE INDEX IDX_ASIENTO_CORRELACION
            ON ASIENTO_CONTABLE (UUID_CORRELACION);
    END IF;
END$$

CALL align_journal_entry_query_indexes()$$
DROP PROCEDURE align_journal_entry_query_indexes$$

DELIMITER ;
