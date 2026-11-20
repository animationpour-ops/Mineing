# Omnimine — « Minecraft mais tout se mine »

Mod **Forge 1.20.1** (47.x). Avec une **pioche en bedrock** portant l'enchantement **Omniminage**, tu peux miner
**n'importe quoi** : des mobs, le **Soleil**, la **Lune**, des **éclairs** et même des **structures entières**.
Tout atterrit dans ton inventaire et se transforme en objets à pouvoirs, avec plein d'effets visuels et de sons.

> ⚠️ Ce mod a été écrit sans pouvoir le compiler ni le lancer (pas de Minecraft dans mon environnement).
> Si GitHub affiche des erreurs, envoie-moi le log rouge et je corrige (comme pour Néant).

---

## 1. Compiler avec GitHub

1. Sur github.com → **New repository** (privé, c'est très bien).
2. Dézippe `omnimine_mod.zip`. Dans le dépôt : **Add file → Upload files**, puis glisse **tout le contenu** du dossier
   (pas le dossier lui-même) : `.github`, `gradle`, `src`, `build.gradle`, `gradle.properties`, `settings.gradle`,
   `gradlew`, `gradlew.bat`, `.gitignore`, `README.md`.
   - Les dossiers qui commencent par un point (`.github`) sont cachés sous Windows/Mac. Si l'onglet **Actions** reste
     vide, crée le fichier à la main : **Add file → Create new file**, nom `.github/workflows/build.yml`, et colle le
     contenu du fichier `build.yml` du zip.
3. **Commit changes**. L'onglet **Actions** lance « Build mod » tout seul (3 à 6 minutes la première fois).
4. Quand c'est vert ✔ : ouvre l'exécution, tout en bas **Artifacts → `omnimine-jar`**, télécharge, dézippe :
   tu obtiens `omnimine-1.0.0.jar`.
5. Mets le `.jar` dans le dossier `mods` de Minecraft **1.20.1 avec Forge 47.3.0 ou plus récent**.
   Pour un serveur, le mod doit être installé côté serveur **et** côté client.
6. Si c'est rouge ✖ : clique sur le job **build** → étape **Compiler**, copie les lignes contenant `error:` et envoie-les.

## 2. Démarrage rapide (tournage)

Commandes (opérateur / triche activée) :

| Commande | Effet |
|---|---|
| `/omnimine kit` | pioche en bedrock enchantée + livre Omniminage |
| `/omnimine tout` | le kit + épée, bottes, armure d'éclair, 8 essences de mobs, 11 structures minées |
| `/omnimine orage` | lance un orage et appelle un éclair devant toi (à miner) |
| `/omnimine ciel` | ramène tout de suite le Soleil et la Lune s'ils ont été minés |
| `/omnimine soleil` · `lune` · `livre` | donne l'épée du Soleil · les bottes lunaires · le livre Omniminage |

## 3. Obtenir la pioche et l'enchantement

- **Pioche en bedrock** : 3 obsidiennes en haut + 2 bâtons (forme d'une pioche normale). Incassable.
- **Enchantement Omniminage** : le **libraire** (villageois bibliothécaire) vend le livre contre **1 bloc d'émeraude**,
  dès le niveau novice (échange garanti : le mod remplace ses échanges de niveau 1 par celui-là et « 24 papiers → 1 émeraude »).
  Applique le livre sur la pioche à l'enclume.
- Sans l'enchantement, la pioche ne mine que les blocs.

## 4. Miner (« tout se mine »)

**Maintiens le clic gauche** : le minage est progressif comme dans Minecraft (barre de progression dans la barre d'action,
fissures qui apparaissent sur la cible, particules). Si tu relâches, la progression retombe. La cible se fige pendant le minage.

| Cible | Comment | Tu obtiens |
|---|---|---|
| **Mob** (squelette, creeper, zombie, araignée, gardien, villageois, golem de fer, enderman) | viser à moins de 5 blocs | une **essence** du mob |
| **Soleil** | viser le Soleil dans le ciel (de jour) | **Épée du Soleil** — le Soleil disparaît, la nuit tombe, il revient après ~90 s |
| **Lune** | viser la Lune (de nuit) | **Bottes Lunaires** |
| **Éclair** | clic droit avec la pioche (ou orage) : l'éclair se fige en l'air ~14 s, vise-le | une pièce de l'**armure d'éclair** (d'abord une pièce que tu n'as pas encore) |
| **Structure** | **accroupi (Shift)** + clic gauche maintenu sur un bloc de la structure | **Structure : X** (la structure se dissout du paysage) |

Temps de minage de base (réglable) : mobs 2–5 s · éclair ~1 s · Lune 6 s · Soleil 7 s · structures 6–10 s.

## 5. Les pouvoirs

**Essences de mobs** (clic droit, 1 pouvoir chacune, avec recharge) :

| Essence | Pouvoir | Recharge |
|---|---|---|
| Squelette | Tempête d'os : pluie d'éclats d'os sur la zone visée | 6 s |
| Creeper | Explosion verte : la zone se charge puis explose | 9 s |
| Zombie | Souffle de peste : brume qui empoisonne, affaiblit et te soigne | 7 s |
| Araignée | Toile géante : fils qui attirent et engluent les ennemis | 8 s |
| Gardien | Rayon : se charge puis perce tout (ennemis et terrain) sur 38 blocs | 10 s |
| Villageois | Cri du village : 3 ondes sonores, verre brisé, émeraudes qui jaillissent | 7 s |
| Golem de fer | Frappe : le sol se fend en étoile, tout est projeté en l'air | 9 s |
| Enderman | Passage de l'ombre : téléportation vers la visée en brisant tout | 6 s |

**Épée du Soleil** : (1) chaque coup embrase la cible et déclenche une éruption solaire ;
(2) clic droit : un rayon du Soleil s'abat sur la visée et laisse un cratère en fusion (recharge 12 s).

**Bottes Lunaires** : (1) gravité lunaire, deux sauts de plus en l'air (appuie sur saut), aucun dégât de chute ;
(2) accroupi en l'air : plongeon et impact lunaire.

**Armure d'éclair** (1 pouvoir par pièce, texture animée sur le corps) :
casque = foudre en chaîne au corps à corps · plastron = décharge statique quand on te frappe (+ immunité à la foudre) ·
jambières = course foudroyante (traînée électrique, la foudre frappe le plus proche).

**Structures évoluées** : une structure minée est inutile seule. Table de craft : la **structure au centre** entourée de
**8 blocs** correspondants (l'infobulle le rappelle). Clic droit sur la structure évoluée = cérémonie de lumière puis items cheat.

| Structure | 8 blocs autour | Items cheat |
|---|---|---|
| Village | blocs de foin | 2 totems, 16 pommes dorées, 8 blocs d'émeraude |
| Avant-poste de pillards | bûches de chêne noir | arbalète (Charge rapide III, Perforation IV), 64 fusées, 1 totem |
| Manoir | planches de chêne noir | 4 totems, 2 pommes dorées enchantées |
| Monument océanique | seaux d'eau (rendus vides) | trident (Empalement V, Loyauté III, Canalisation), 2 conduits, cœur de la mer |
| Pyramide du désert | grès | 6 pommes dorées enchantées, 32 TNT |
| Temple de la jungle | pierre moussue | 32 pommes dorées, livre Réparation |
| Forteresse du Nether | briques du Nether | 4 lingots de netherite, 3 crânes de wither squelette |
| Bastion | pierre noire | 6 lingots de netherite, 16 blocs d'or, 1 pomme dorée enchantée |
| Cité de l'End | blocs de purpur | élytres, 64 fusées, 2 shulker boxes, tête de dragon |
| Forteresse (Stronghold) | pierre taillée | 12 yeux d'ender, 16 perles, 1 totem |
| Cité antique | deepslate taillée | livre Réparation, 8 éclats d'écho, 2 totems, 2 pommes dorées enchantées |

## 6. Configuration

Fichier `config/omnimine-common.toml` (créé au premier lancement) :

| Option | Défaut | Rôle |
|---|---|---|
| `miningSpeed` | 1.0 | vitesse du minage (2.0 = deux fois plus vite) |
| `boltFreezeSeconds` | 14 | durée d'un éclair figé avant de frapper pour de vrai |
| `boltCaptureRadius` | 64 | rayon de capture des éclairs autour d'un joueur qui tient la pioche |
| `dissolveBlocksPerTick` | 2500 | vitesse de dissolution d'une structure minée |
| `sunReturnSeconds` / `moonReturnSeconds` | 90 | retour du Soleil / de la Lune minés |
| `forceNightWithoutSun` | true | sans Soleil, la nuit tombe |
| `terrainDestruction` | true | les pouvoirs détruisent le décor (mets `false` pour ne rien casser) |

## 7. Ce que j'ai supposé (à me dire si tu veux autre chose)

- Pioche craftable avec 3 obsidiennes + 2 bâtons (pas cher, comme demandé).
- L'enchantement s'achète en livre chez le libraire, 1 bloc d'émeraude, échange garanti au niveau novice.
- Seuls les 8 mobs demandés se minent ; les autres mobs ne réagissent pas à la pioche.
- Pour les structures, il faut être **accroupi** (sinon le clic gauche mine un bloc normalement) ; la structure doit
  avoir été générée par le jeu (village, avant-poste, manoir, monument, pyramide, temple, forteresse du Nether, bastion,
  cité de l'End, forteresse, cité antique).
- Les icônes reprennent les formes vanilla (pioche en pierre, épée dorée, bottes/casque/plastron/jambières en fer)
  repeintes ; l'armure portée garde le modèle vanilla avec un nouveau dessin animé (orage). Les essences et structures
  ont leurs propres icônes (aucun personnage Minecraft dessiné).
- Tout est calculé côté serveur : le mod fonctionne aussi sur un serveur dédié.
