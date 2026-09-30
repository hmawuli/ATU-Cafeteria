/// Money decision helpers shared by checkout and wallet screens.
library;

/// Whether [balance] is insufficient to cover [total] (float-safe epsilon).
bool isUnderfunded(double balance, double total) => balance + 1e-9 < total;

/// How much more the wallet needs to cover [total].
double fundingShortage(double balance, double total) =>
    total > balance ? total - balance : 0;