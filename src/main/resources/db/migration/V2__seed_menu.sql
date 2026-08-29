INSERT INTO menu_items (id, parent_id, menu_key, icon, route, sort_order)
VALUES (1, NULL, 'menu.dashboard', 'dashboard', '/dashboard', 0),
       (2, NULL, 'menu.debts', 'receipt_long', '/debts', 1),
       (3, NULL, 'menu.debtors', 'group', '/debtors', 2),
       (4, NULL, 'menu.documents', 'description', '/documents', 3),
       (5, NULL, 'menu.reports', 'summarize', '/reports', 4),
       (6, NULL, 'menu.admin', 'admin_panel_settings', NULL, 9),
       (7, 6, 'menu.admin.menus', 'list_alt', '/admin/menus', 0),
       (8, 6, 'menu.admin.installments', 'tune', '/admin/installment-choices', 1),
       (9, 6, 'menu.admin.logs', 'history', '/admin/login-logs', 2),
       (10, 6, 'menu.admin.creditors', 'supervisor_account', '/admin/creditors', 3),
       (11, NULL, 'menu.profile', 'person', '/profile', 10);

SELECT setval(pg_get_serial_sequence('menu_items', 'id'), (SELECT max(id) FROM menu_items));

INSERT INTO menu_permissions (menu_item_id, role)
VALUES (1, 'ADMIN'), (1, 'CREDITOR'), (1, 'DEBTOR'),
       (2, 'ADMIN'), (2, 'CREDITOR'), (2, 'DEBTOR'),
       (3, 'ADMIN'), (3, 'CREDITOR'),
       (4, 'ADMIN'), (4, 'CREDITOR'),
       (5, 'ADMIN'), (5, 'CREDITOR'), (5, 'DEBTOR'),
       (6, 'ADMIN'),
       (7, 'ADMIN'),
       (8, 'ADMIN'),
       (9, 'ADMIN'),
       (10, 'ADMIN'),
       (11, 'ADMIN'), (11, 'CREDITOR'), (11, 'DEBTOR');
