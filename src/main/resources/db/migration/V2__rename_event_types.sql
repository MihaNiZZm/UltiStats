UPDATE events
SET event_type = 'INCOMPLETE_PASS'
WHERE event_type = 'DROP';

UPDATE events
SET event_type = 'PICKUP'
WHERE event_type = 'TURNOVER';
