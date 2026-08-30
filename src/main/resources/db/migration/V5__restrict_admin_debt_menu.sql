DELETE FROM menu_permissions
WHERE role = 'ADMIN'
  AND menu_item_id IN (
      SELECT id FROM menu_items WHERE menu_key IN ('menu.debts', 'menu.reports')
  );
