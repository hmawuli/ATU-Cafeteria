<?php

namespace App\Support;

use App\Models\MenuItem;

/**
 * Health/sustainability badges derived from a menu item's dietary tags and
 * allergen data, so clients (and the open "what's open" page) can surface
 * them without re-implementing the logic.
 */
final class MenuBadges
{
    /**
     * @return array<int, string>
     */
    public static function for(MenuItem $item): array
    {
        $tags = array_map('strtolower', (array) ($item->dietary_tags ?? []));
        $allergens = strtolower((string) $item->allergen_info);

        $badges = [];

        if (in_array('vegan', $tags, true)) {
            $badges[] = 'VEGAN';
        } elseif (in_array('vegetarian', $tags, true)) {
            $badges[] = 'VEGETARIAN';
        }

        if (! str_contains($allergens, 'gluten')) {
            $badges[] = 'GLUTEN-FREE';
        }

        if ((bool) $item->is_featured) {
            $badges[] = 'FEATURED';
        }

        return $badges;
    }
}
