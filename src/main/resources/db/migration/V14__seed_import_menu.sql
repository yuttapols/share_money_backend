INSERT INTO menu_items (parent_id, menu_key, icon, route, sort_order)
VALUES (6, 'menu.admin.import', 'upload_file', '/admin/import', 4);

INSERT INTO menu_permissions (menu_item_id, role)
SELECT id, 'ADMIN' FROM menu_items WHERE menu_key = 'menu.admin.import';
