<?php

return [
    'roles' => [
        'SUPER_ADMIN' => '*',
        'CAFETERIA_ADMIN' => [
            'dashboard.view', 'users.view', 'users.update', 'users.suspend',
            'vendors.view', 'vendors.approve', 'vendors.suspend',
            'menu.view', 'menu.approve', 'menu.update',
            'orders.view', 'orders.resolve', 'inventory.view', 'inventory.manage', 'refunds.view', 'refunds.manage', 'promotions.view', 'promotions.manage', 'settlements.view', 'settlements.manage', 'support.view', 'support.manage',
            'reports.view', 'feedback.view', 'feedback.resolve', 'audit.view', 'smart.command_center.view', 'security.alerts.view',
        ],
        'FINANCE_ADMIN' => [
            'dashboard.view', 'payments.view', 'payments.adjust', 'refunds.view', 'refunds.manage', 'settlements.view', 'settlements.manage', 'reports.view', 'audit.view', 'smart.command_center.view', 'security.alerts.view',
        ],
    ],
    'student' => [
        'dashboard.view', 'orders.view', 'orders.create', 'wallet.view', 'wallet.transact', 'feedback.create', 'recommendations.view', 'queue.view',
    ],
    'vendor' => [
        'dashboard.view', 'menu.view', 'menu.update', 'orders.view', 'orders.update', 'feedback.view', 'feedback.resolve', 'reports.view', 'queue.view', 'demand_forecast.view', 'waste.manage',
    ],
];
