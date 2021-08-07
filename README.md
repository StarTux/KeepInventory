# KeepInventory

Keep your inventory upon death. You can opt out of this.

## Commands

- `/keepinventory` View opt-out status
- `/keepinventory off` Opt out
- `/keepinventory on` Opt back in (the default)

## Permissions

- `keepinventory.keepinventory` Use the `/keepinventory` command. This
  permission does not affect inventory keeping.

## Mechanics

If opted out, nothing happens.

If you die while opted in, the all items in your inventory are
kept. Exp is kept based on world difficulty:
- **Peaceful** and **Easy**: Keep all exp and levels
- **Normal**: Lose one level and all exp
- **Hard**: Vanilla behavior remains unchanged

## Issues

The `keepInventory` game rule must be disabled in your world, or else
this plugin will ignore you.

## Data storage

Opt-outs are stored in `optouts.json` in the plugin data folder.
