import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/theme/app_theme.dart';

/// Mobile-first brand header used across ATU home screens.
///
/// Always shows: logo tile + "ATU CAFETERIA" wordmark + cart (with live badge).
/// When [signedIn] is false it shows a "Sign in" pill; when true it shows a
/// notifications bell and a profile avatar instead — so the same widget can be
/// reused by the public landing (signed out) and the student home (signed in).
class HomeHeader extends StatelessWidget {
  const HomeHeader({
    super.key,
    required this.cartCount,
    required this.signedIn,
    required this.onCart,
    required this.onSignIn,
    this.profileInitials,
    this.onBell,
    this.onProfile,
  });

  /// Number of items in the cart (badge is hidden when <= 0).
  final int cartCount;

  /// Whether the current user is authenticated.
  final bool signedIn;

  final VoidCallback onCart;
  final VoidCallback onSignIn;

  /// Optional initials shown in the profile avatar (falls back to an icon).
  final String? profileInitials;
  final VoidCallback? onBell;
  final VoidCallback? onProfile;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppTheme.primary,
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 10, 12, 10),
          child: Row(
            children: [
              _LogoTile(),
              const SizedBox(width: 10),
              const Flexible(
                child: Text(
                  'ATU CAFETERIA',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 17,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 0.5,
                  ),
                ),
              ),
              const Spacer(),
              _CartButton(count: cartCount, onPressed: onCart),
              if (signedIn) ...[
                IconButton(
                  tooltip: 'Notifications',
                  onPressed: onBell,
                  icon: const Icon(
                    Icons.notifications_none_rounded,
                    color: Colors.white,
                  ),
                ),
                _ProfileAvatar(initials: profileInitials, onTap: onProfile),
              ] else ...[
                const SizedBox(width: 4),
                _SignInPill(onPressed: onSignIn),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _LogoTile extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      width: 38,
      height: 38,
      decoration: BoxDecoration(
        color: AppTheme.accent,
        borderRadius: BorderRadius.circular(10),
      ),
      child: const Icon(
        Icons.restaurant_menu_rounded,
        color: AppTheme.primary,
        size: 24,
      ),
    );
  }
}

class _CartButton extends StatelessWidget {
  const _CartButton({required this.count, required this.onPressed});

  final int count;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Badge(
      isLabelVisible: count > 0,
      label: Text('$count'),
      child: IconButton(
        tooltip: 'View cart',
        onPressed: onPressed,
        icon: const Icon(Icons.shopping_cart_outlined, color: Colors.white),
      ),
    );
  }
}

class _SignInPill extends StatelessWidget {
  const _SignInPill({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed,
      style: TextButton.styleFrom(
        backgroundColor: AppTheme.accent,
        foregroundColor: AppTheme.primaryDark,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
        minimumSize: Size.zero,
        shape: const StadiumBorder(),
        textStyle: const TextStyle(fontWeight: FontWeight.w800, fontSize: 13),
      ),
      child: const Text('Sign in'),
    );
  }
}

class _ProfileAvatar extends StatelessWidget {
  const _ProfileAvatar({this.initials, this.onTap});

  final String? initials;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final label = (initials ?? '').trim();
    return InkWell(
      onTap: onTap,
      customBorder: const CircleBorder(),
      child: Container(
        width: 36,
        height: 36,
        decoration: const BoxDecoration(
          color: AppTheme.accent,
          shape: BoxShape.circle,
        ),
        alignment: Alignment.center,
        child: label.isEmpty
            ? const Icon(Icons.person, color: AppTheme.primaryDark, size: 22)
            : Text(
                label,
                style: const TextStyle(
                  color: AppTheme.primaryDark,
                  fontWeight: FontWeight.w900,
                  fontSize: 13,
                ),
              ),
      ),
    );
  }
}
