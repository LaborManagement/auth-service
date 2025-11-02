SET search_path TO auth;
\set ON_ERROR_STOP on
BEGIN;

-- View board requests
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
VALUES ('View Board Requests', 'read', 1, (SELECT id FROM capabilities WHERE name = 'board.request.read'), 10, true, NOW(), NOW(), 'list', 'default');

-- View receipts
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
VALUES ('View Receipts', 'read', 1, (SELECT id FROM capabilities WHERE name = 'board.receipt.read'), 11, true, NOW(), NOW(), 'receipt', 'default');

-- Approve payments
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
VALUES ('Approve Payments', 'approve', 1, (SELECT id FROM capabilities WHERE name = 'board.payment.approve'), 12, true, NOW(), NOW(), 'check-circle', 'success');

-- Reject payments
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
VALUES ('Reject Payments', 'reject', 1, (SELECT id FROM capabilities WHERE name = 'board.payment.reject'), 13, true, NOW(), NOW(), 'x-circle', 'danger');

-- Reconcile payments
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
VALUES ('Reconcile Payments', 'reconcile', 1, (SELECT id FROM capabilities WHERE name = 'board.payment.reconcile'), 14, true, NOW(), NOW(), 'sync', 'primary');

-- Process receipts
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
VALUES ('Process Receipts', 'process', 1, (SELECT id FROM capabilities WHERE name = 'board.receipt.process'), 15, true, NOW(), NOW(), 'inbox', 'info');

-- Vote on decision
INSERT INTO page_actions (label, action, page_id, capability_id, display_order, is_active, created_at, updated_at, icon, variant)
VALUES ('Vote on Decision', 'vote', 1, (SELECT id FROM capabilities WHERE name = 'board.decision.vote'), 16, true, NOW(), NOW(), 'thumbs-up', 'warning');

-- Show what was created
SELECT 
  pa.id,
  pa.label,
  pa.action,
  c.name as capability,
  up.label as page
FROM page_actions pa
JOIN capabilities c ON pa.capability_id = c.id
JOIN ui_pages up ON pa.page_id = up.id
WHERE c.name LIKE 'board.%'
ORDER BY pa.id;

COMMIT;
