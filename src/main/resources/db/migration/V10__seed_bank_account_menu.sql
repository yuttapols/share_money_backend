INSERT INTO menu_items (id, parent_id, menu_key, icon, route, sort_order)
VALUES (12, NULL, 'menu.bankAccount', 'account_balance', '/bank-accounts', 5);

SELECT setval(pg_get_serial_sequence('menu_items', 'id'), (SELECT max(id) FROM menu_items));

INSERT INTO menu_permissions (menu_item_id, role)
VALUES (12, 'CREDITOR');
