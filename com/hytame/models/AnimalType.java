package com.hytame.models;

public enum AnimalType {
   COW(AnimalType.Category.LIVESTOCK, "Cow", "Plant_Crop_Cauliflower_Item", "Calf", "Cow_Calf"),
   PIG(AnimalType.Category.LIVESTOCK, "Pig", "Plant_Crop_Mushroom_Cap_Brown", "Piglet", "Pig_Piglet"),
   CHICKEN(AnimalType.Category.LIVESTOCK, "Chicken", "Plant_Crop_Corn_Item", "Chick", "Chicken_Chick"),
   SHEEP(AnimalType.Category.LIVESTOCK, "Sheep", "Plant_Crop_Lettuce_Item", "Lamb", "Sheep_Lamb"),
   GOAT(AnimalType.Category.LIVESTOCK, "Goat", "Plant_Fruit_Apple", "Goat_Kid", "Goat_Kid"),
   HORSE(AnimalType.Category.LIVESTOCK, "Horse", "Plant_Crop_Carrot_Item", "Horse_Foal", "Horse_Foal"),
   CAMEL(AnimalType.Category.LIVESTOCK, "Camel", "Plant_Crop_Wheat_Item", "Camel_Calf", "Camel_Calf"),
   RAM(AnimalType.Category.LIVESTOCK, "Ram", "Plant_Fruit_Apple", "Ram_Lamb", "Ram_Lamb"),
   TURKEY(AnimalType.Category.LIVESTOCK, "Turkey", "Plant_Crop_Corn_Item", "Turkey_Chick", "Turkey_Chick"),
   BOAR(AnimalType.Category.LIVESTOCK, "Boar", "Plant_Crop_Mushroom_Cap_Red", "Boar_Piglet", "Boar_Piglet"),
   RABBIT(AnimalType.Category.LIVESTOCK, "Rabbit", "Plant_Crop_Carrot_Item", "Bunny", "Bunny"),
   BISON(AnimalType.Category.LIVESTOCK, "Bison", "Plant_Crop_Wheat_Item", "Bison_Calf", "Bison_Calf"),
   CHICKEN_DESERT(AnimalType.Category.LIVESTOCK, "Chicken_Desert", "Plant_Crop_Corn_Item", "Chicken_Desert_Chick", "Chicken_Desert_Chick"),
   MOUFLON(AnimalType.Category.LIVESTOCK, "Mouflon", "Plant_Crop_Lettuce_Item", "Mouflon_Lamb", "Mouflon_Lamb"),
   PIG_WILD(AnimalType.Category.LIVESTOCK, "Pig_Wild", "Plant_Crop_Mushroom_Cap_Brown", "Pig_Wild_Piglet", "Pig_Wild_Piglet"),
   SKRILL(AnimalType.Category.LIVESTOCK, "Skrill", "Plant_Crop_Corn_Item", "Skrill_Chick", "Skrill_Chick"),
   WARTHOG(AnimalType.Category.LIVESTOCK, "Warthog", "Plant_Crop_Mushroom_Cap_Red", "Warthog_Piglet", "Warthog_Piglet"),
   WOLF(AnimalType.Category.MAMMAL, "Wolf_Black", "Food_Wildmeat_Cooked", (String)null, (String)null),
   WOLF_WHITE(AnimalType.Category.MAMMAL, "Wolf_White", "Food_Wildmeat_Cooked", (String)null, (String)null),
   FOX(AnimalType.Category.MAMMAL, "Fox", "Food_Wildmeat_Raw", (String)null, (String)null),
   BEAR_GRIZZLY(AnimalType.Category.MAMMAL, "Bear_Grizzly", "Plant_Fruit_Apple", (String)null, (String)null),
   BEAR_POLAR(AnimalType.Category.MAMMAL, "Bear_Polar", "Food_Fish_Grilled", (String)null, (String)null),
   DEER_DOE(AnimalType.Category.MAMMAL, "Deer_Doe", "Plant_Crop_Carrot_Item", (String)null, (String)null),
   DEER_STAG(AnimalType.Category.MAMMAL, "Deer_Stag", "Plant_Crop_Carrot_Item", (String)null, (String)null),
   MOOSE_BULL(AnimalType.Category.MAMMAL, "Moose_Bull", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   MOOSE_COW(AnimalType.Category.MAMMAL, "Moose_Cow", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   HYENA(AnimalType.Category.MAMMAL, "Hyena", "Food_Wildmeat_Raw", (String)null, (String)null),
   ANTELOPE(AnimalType.Category.MAMMAL, "Antelope", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   ARMADILLO(AnimalType.Category.MAMMAL, "Armadillo", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   LEOPARD_SNOW(AnimalType.Category.MAMMAL, "Leopard_Snow", "Food_Wildmeat_Raw", (String)null, (String)null),
   MOSSHORN(AnimalType.Category.MAMMAL, "Mosshorn", "Plant_Crop_Lettuce_Item", (String)null, (String)null),
   TIGER_SABERTOOTH(AnimalType.Category.MAMMAL, "Tiger_Sabertooth", "Food_Wildmeat_Raw", (String)null, (String)null),
   FROG(AnimalType.Category.CRITTER, "Frog_Green", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   GECKO(AnimalType.Category.CRITTER, "Gecko", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   MEERKAT(AnimalType.Category.CRITTER, "Meerkat", "Food_Wildmeat_Raw", (String)null, (String)null),
   MOUSE(AnimalType.Category.CRITTER, "Mouse", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   SQUIRREL(AnimalType.Category.CRITTER, "Squirrel", "Plant_Fruit_Apple", (String)null, (String)null),
   DUCK(AnimalType.Category.AVIAN, "Duck", "Plant_Crop_Corn_Item", (String)null, (String)null),
   PIGEON(AnimalType.Category.AVIAN, "Pigeon", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   BAT(AnimalType.Category.AVIAN, "Bat", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   BAT_ICE(AnimalType.Category.AVIAN, "Bat_Ice", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   BLUEBIRD(AnimalType.Category.AVIAN, "Bluebird", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   CROW(AnimalType.Category.AVIAN, "Crow", "Plant_Crop_Corn_Item", (String)null, (String)null),
   FINCH_GREEN(AnimalType.Category.AVIAN, "Finch_Green", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   FLAMINGO(AnimalType.Category.AVIAN, "Flamingo", "Food_Fish_Raw", (String)null, (String)null),
   OWL_BROWN(AnimalType.Category.AVIAN, "Owl_Brown", "Food_Wildmeat_Raw", (String)null, (String)null),
   OWL_SNOW(AnimalType.Category.AVIAN, "Owl_Snow", "Food_Wildmeat_Raw", (String)null, (String)null),
   PARROT(AnimalType.Category.AVIAN, "Parrot", "Plant_Crop_Corn_Item", (String)null, (String)null),
   PENGUIN(AnimalType.Category.AVIAN, "Penguin", "Food_Fish_Raw", (String)null, (String)null),
   RAVEN(AnimalType.Category.AVIAN, "Raven", "Plant_Crop_Corn_Item", (String)null, (String)null),
   SPARROW(AnimalType.Category.AVIAN, "Sparrow", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   WOODPECKER(AnimalType.Category.AVIAN, "Woodpecker", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   HAWK(AnimalType.Category.AVIAN, "Hawk", "Food_Wildmeat_Raw", (String)null, (String)null),
   VULTURE(AnimalType.Category.AVIAN, "Vulture", "Food_Wildmeat_Raw", (String)null, (String)null),
   TETRABIRD(AnimalType.Category.AVIAN, "Tetrabird", "Food_Wildmeat_Raw", (String)null, (String)null),
   TORTOISE(AnimalType.Category.REPTILE, "Tortoise", "Plant_Crop_Lettuce_Item", (String)null, (String)null),
   CROCODILE(AnimalType.Category.REPTILE, "Crocodile", "Food_Wildmeat_Raw", (String)null, (String)null),
   LIZARD_SAND(AnimalType.Category.REPTILE, "Lizard_Sand", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   TOAD_RHINO(AnimalType.Category.REPTILE, "Toad_Rhino", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   TOAD_RHINO_MAGMA(AnimalType.Category.REPTILE, "Toad_Rhino_Magma", "Plant_Fruit_Berries_Red", (String)null, (String)null),
   RAT(AnimalType.Category.VERMIN, "Rat", "Plant_Crop_Wheat_Item", (String)null, (String)null),
   MOLERAT(AnimalType.Category.VERMIN, "Molerat", "Plant_Crop_Carrot_Item", (String)null, (String)null),
   LARVA_SILK(AnimalType.Category.VERMIN, "Larva_Silk", "Plant_Crop_Lettuce_Item", (String)null, (String)null),
   SCORPION(AnimalType.Category.VERMIN, "Scorpion", "Food_Wildmeat_Raw", (String)null, (String)null),
   SLUG_MAGMA(AnimalType.Category.VERMIN, "Slug_Magma", "Plant_Crop_Mushroom_Cap_Red", (String)null, (String)null),
   SNAIL_FROST(AnimalType.Category.VERMIN, "Snail_Frost", "Plant_Crop_Lettuce_Item", (String)null, (String)null),
   SNAIL_MAGMA(AnimalType.Category.VERMIN, "Snail_Magma", "Plant_Crop_Mushroom_Cap_Red", (String)null, (String)null),
   SNAKE_COBRA(AnimalType.Category.VERMIN, "Snake_Cobra", "Food_Wildmeat_Raw", (String)null, (String)null),
   SNAKE_MARSH(AnimalType.Category.VERMIN, "Snake_Marsh", "Food_Wildmeat_Raw", (String)null, (String)null),
   SNAKE_RATTLE(AnimalType.Category.VERMIN, "Snake_Rattle", "Food_Wildmeat_Raw", (String)null, (String)null),
   SPIDER(AnimalType.Category.VERMIN, "Spider", "Food_Wildmeat_Raw", (String)null, (String)null),
   SPIDER_CAVE(AnimalType.Category.VERMIN, "Spider_Cave", "Food_Wildmeat_Raw", (String)null, (String)null),
   EEL_MORAY(AnimalType.Category.AQUATIC, "Eel_Moray", "Food_Fish_Raw", (String)null, (String)null),
   SHARK_HAMMERHEAD(AnimalType.Category.AQUATIC, "Shark_Hammerhead", "Food_Fish_Raw", (String)null, (String)null),
   SHELLFISH_LAVA(AnimalType.Category.AQUATIC, "Shellfish_Lava", "Food_Fish_Raw", (String)null, (String)null),
   TRILOBITE(AnimalType.Category.AQUATIC, "Trilobite", "Food_Fish_Raw", (String)null, (String)null),
   TRILOBITE_BLACK(AnimalType.Category.AQUATIC, "Trilobite_Black", "Food_Fish_Raw", (String)null, (String)null),
   WHALE_HUMPBACK(AnimalType.Category.AQUATIC, "Whale_Humpback", "Food_Fish_Raw", (String)null, (String)null),
   BLUEGILL(AnimalType.Category.AQUATIC, "Bluegill", "Food_Fish_Raw", (String)null, (String)null),
   CATFISH(AnimalType.Category.AQUATIC, "Catfish", "Food_Fish_Raw", (String)null, (String)null),
   FROSTGILL(AnimalType.Category.AQUATIC, "Frostgill", "Food_Fish_Raw", (String)null, (String)null),
   MINNOW(AnimalType.Category.AQUATIC, "Minnow", "Food_Fish_Raw", (String)null, (String)null),
   PIKE(AnimalType.Category.AQUATIC, "Pike", "Food_Fish_Raw", (String)null, (String)null),
   PIRANHA(AnimalType.Category.AQUATIC, "Piranha", "Food_Fish_Raw", (String)null, (String)null),
   PIRANHA_BLACK(AnimalType.Category.AQUATIC, "Piranha_Black", "Food_Fish_Raw", (String)null, (String)null),
   SALMON(AnimalType.Category.AQUATIC, "Salmon", "Food_Fish_Raw", (String)null, (String)null),
   SNAPJAW(AnimalType.Category.AQUATIC, "Snapjaw", "Food_Fish_Raw", (String)null, (String)null),
   TROUT_RAINBOW(AnimalType.Category.AQUATIC, "Trout_Rainbow", "Food_Fish_Raw", (String)null, (String)null),
   CLOWNFISH(AnimalType.Category.AQUATIC, "Clownfish", "Food_Fish_Raw", (String)null, (String)null),
   CRAB(AnimalType.Category.AQUATIC, "Crab", "Food_Fish_Raw", (String)null, (String)null),
   JELLYFISH_BLUE(AnimalType.Category.AQUATIC, "Jellyfish_Blue", "Food_Fish_Raw", (String)null, (String)null),
   JELLYFISH_CYAN(AnimalType.Category.AQUATIC, "Jellyfish_Cyan", "Food_Fish_Raw", (String)null, (String)null),
   JELLYFISH_GREEN(AnimalType.Category.AQUATIC, "Jellyfish_Green", "Food_Fish_Raw", (String)null, (String)null),
   JELLYFISH_MAN_OF_WAR(AnimalType.Category.AQUATIC, "Jellyfish_Man_Of_War", "Food_Fish_Raw", (String)null, (String)null),
   JELLYFISH_RED(AnimalType.Category.AQUATIC, "Jellyfish_Red", "Food_Fish_Raw", (String)null, (String)null),
   JELLYFISH_YELLOW(AnimalType.Category.AQUATIC, "Jellyfish_Yellow", "Food_Fish_Raw", (String)null, (String)null),
   LOBSTER(AnimalType.Category.AQUATIC, "Lobster", "Food_Fish_Raw", (String)null, (String)null),
   PUFFERFISH(AnimalType.Category.AQUATIC, "Pufferfish", "Food_Fish_Raw", (String)null, (String)null),
   TANG_BLUE(AnimalType.Category.AQUATIC, "Tang_Blue", "Food_Fish_Raw", (String)null, (String)null),
   TANG_CHEVRON(AnimalType.Category.AQUATIC, "Tang_Chevron", "Food_Fish_Raw", (String)null, (String)null),
   TANG_LEMON_PEEL(AnimalType.Category.AQUATIC, "Tang_Lemon_Peel", "Food_Fish_Raw", (String)null, (String)null),
   TANG_SAILFIN(AnimalType.Category.AQUATIC, "Tang_Sailfin", "Food_Fish_Raw", (String)null, (String)null),
   EMBERWULF(AnimalType.Category.MYTHIC, "Emberwulf", "Food_Wildmeat_Cooked", (String)null, (String)null),
   YETI(AnimalType.Category.MYTHIC, "Yeti", "Food_Wildmeat_Raw", (String)null, (String)null),
   FEN_STALKER(AnimalType.Category.MYTHIC, "Fen_Stalker", "Food_Wildmeat_Raw", (String)null, (String)null),
   CACTEE(AnimalType.Category.MYTHIC, "Cactee", "Plant_Cactus_Flower", (String)null, (String)null),
   HATWORM(AnimalType.Category.MYTHIC, "Hatworm", "Plant_Crop_Mushroom_Cap_Brown", (String)null, (String)null),
   SNAPDRAGON(AnimalType.Category.MYTHIC, "Snapdragon", "Food_Wildmeat_Raw", (String)null, (String)null),
   SPARK_LIVING(AnimalType.Category.MYTHIC, "Spark_Living", "Plant_Crop_Chilli_Item", (String)null, (String)null),
   TRILLODON(AnimalType.Category.MYTHIC, "Trillodon", "Food_Wildmeat_Raw", (String)null, (String)null),
   RAPTOR_CAVE(AnimalType.Category.DINOSAUR, "Raptor_Cave", "Food_Wildmeat_Cooked", (String)null, (String)null),
   REX_CAVE(AnimalType.Category.DINOSAUR, "Rex_Cave", "Food_Wildmeat_Cooked", (String)null, (String)null),
   ARCHAEOPTERYX(AnimalType.Category.DINOSAUR, "Archaeopteryx", "Food_Wildmeat_Raw", (String)null, (String)null),
   PTERODACTYL(AnimalType.Category.DINOSAUR, "Pterodactyl", "Food_Fish_Raw", (String)null, (String)null),
   DRAGON_FIRE(AnimalType.Category.BOSS, "Dragon_Fire", "Food_Wildmeat_Cooked", (String)null, (String)null),
   DRAGON_FROST(AnimalType.Category.BOSS, "Dragon_Frost", "Food_Fish_Raw", (String)null, (String)null),
   SKELETON(AnimalType.Category.UNDEAD, "Skeleton", "Ingredient_Bone_Fragment", (String)null, (String)null),
   ZOMBIE(AnimalType.Category.UNDEAD, "Zombie", "Food_Wildmeat_Raw", (String)null, (String)null),
   ZOMBIE_BURNT(AnimalType.Category.UNDEAD, "Zombie_Burnt", "Food_Wildmeat_Raw", (String)null, (String)null),
   ZOMBIE_FROST(AnimalType.Category.UNDEAD, "Zombie_Frost", "Food_Wildmeat_Raw", (String)null, (String)null),
   ZOMBIE_SAND(AnimalType.Category.UNDEAD, "Zombie_Sand", "Food_Wildmeat_Raw", (String)null, (String)null),
   ZOMBIE_ABERRANT(AnimalType.Category.UNDEAD, "Zombie_Aberrant", "Food_Wildmeat_Raw", (String)null, (String)null),
   GHOUL(AnimalType.Category.UNDEAD, "Ghoul", "Food_Wildmeat_Raw", (String)null, (String)null),
   WRAITH(AnimalType.Category.UNDEAD, "Wraith", "Ingredient_Void_Essence", (String)null, (String)null),
   WEREWOLF(AnimalType.Category.UNDEAD, "Werewolf", "Food_Wildmeat_Raw", (String)null, (String)null),
   SHADOW_KNIGHT(AnimalType.Category.UNDEAD, "Shadow_Knight", "Ingredient_Bone_Fragment", (String)null, (String)null),
   HORSE_SKELETON(AnimalType.Category.UNDEAD, "Horse_Skeleton", "Ingredient_Bone_Fragment", (String)null, (String)null),
   HOUND_BLEACHED(AnimalType.Category.UNDEAD, "Hound_Bleached", "Ingredient_Bone_Fragment", (String)null, (String)null),
   CHICKEN_UNDEAD(AnimalType.Category.UNDEAD, "Chicken_Undead", "Ingredient_Bone_Fragment", (String)null, (String)null),
   COW_UNDEAD(AnimalType.Category.UNDEAD, "Cow_Undead", "Ingredient_Bone_Fragment", (String)null, (String)null),
   PIG_UNDEAD(AnimalType.Category.UNDEAD, "Pig_Undead", "Ingredient_Bone_Fragment", (String)null, (String)null),
   GOLEM_CRYSTAL_EARTH(AnimalType.Category.GOLEM, "Golem_Crystal_Earth", "Ingredient_Crystal_Purple", (String)null, (String)null),
   GOLEM_CRYSTAL_FLAME(AnimalType.Category.GOLEM, "Golem_Crystal_Flame", "Ingredient_Crystal_Purple", (String)null, (String)null),
   GOLEM_CRYSTAL_FROST(AnimalType.Category.GOLEM, "Golem_Crystal_Frost", "Ingredient_Crystal_Purple", (String)null, (String)null),
   GOLEM_CRYSTAL_SAND(AnimalType.Category.GOLEM, "Golem_Crystal_Sand", "Ingredient_Crystal_Purple", (String)null, (String)null),
   GOLEM_CRYSTAL_THUNDER(AnimalType.Category.GOLEM, "Golem_Crystal_Thunder", "Ingredient_Crystal_Purple", (String)null, (String)null),
   GOLEM_FIRESTEEL(AnimalType.Category.GOLEM, "Golem_Firesteel", "Ingredient_Bar_Iron", (String)null, (String)null),
   SPIRIT_EMBER(AnimalType.Category.SPIRIT, "Spirit_Ember", "Plant_Crop_Chilli_Item", (String)null, (String)null),
   SPIRIT_FROST(AnimalType.Category.SPIRIT, "Spirit_Frost", "Ingredient_Ice_Essence", (String)null, (String)null),
   SPIRIT_ROOT(AnimalType.Category.SPIRIT, "Spirit_Root", "Plant_Fruit_Apple", (String)null, (String)null),
   SPIRIT_THUNDER(AnimalType.Category.SPIRIT, "Spirit_Thunder", "Ingredient_Crystal_Purple", (String)null, (String)null),
   GOBLIN_SCRAPPER(AnimalType.Category.GOBLIN, "Goblin_Scrapper", "Food_Wildmeat_Raw", (String)null, (String)null),
   GOBLIN_THIEF(AnimalType.Category.GOBLIN, "Goblin_Thief", "Food_Wildmeat_Raw", (String)null, (String)null),
   GOBLIN_MINER(AnimalType.Category.GOBLIN, "Goblin_Miner", "Food_Wildmeat_Raw", (String)null, (String)null),
   GOBLIN_LOBBER(AnimalType.Category.GOBLIN, "Goblin_Lobber", "Food_Wildmeat_Raw", (String)null, (String)null),
   GOBLIN_SCAVENGER(AnimalType.Category.GOBLIN, "Goblin_Scavenger", "Food_Wildmeat_Raw", (String)null, (String)null),
   GOBLIN_HERMIT(AnimalType.Category.GOBLIN, "Goblin_Hermit", "Food_Wildmeat_Raw", (String)null, (String)null),
   GOBLIN_OGRE(AnimalType.Category.GOBLIN, "Goblin_Ogre", "Food_Wildmeat_Raw", (String)null, (String)null),
   GOBLIN_DUKE(AnimalType.Category.GOBLIN, "Goblin_Duke", "Food_Wildmeat_Cooked", (String)null, (String)null),
   TRORK_BRAWLER(AnimalType.Category.TRORK, "Trork_Brawler", "Food_Wildmeat_Raw", (String)null, (String)null),
   TRORK_WARRIOR(AnimalType.Category.TRORK, "Trork_Warrior", "Food_Wildmeat_Raw", (String)null, (String)null),
   TRORK_HUNTER(AnimalType.Category.TRORK, "Trork_Hunter", "Food_Wildmeat_Raw", (String)null, (String)null),
   TRORK_SENTRY(AnimalType.Category.TRORK, "Trork_Sentry", "Food_Wildmeat_Raw", (String)null, (String)null),
   TRORK_GUARD(AnimalType.Category.TRORK, "Trork_Guard", "Food_Wildmeat_Raw", (String)null, (String)null),
   TRORK_MAULER(AnimalType.Category.TRORK, "Trork_Mauler", "Food_Wildmeat_Raw", (String)null, (String)null),
   TRORK_SHAMAN(AnimalType.Category.TRORK, "Trork_Shaman", "Food_Wildmeat_Raw", (String)null, (String)null),
   TRORK_CHIEFTAIN(AnimalType.Category.TRORK, "Trork_Chieftain", "Food_Wildmeat_Cooked", (String)null, (String)null),
   TRORK_DOCTOR_WITCH(AnimalType.Category.TRORK, "Trork_Doctor_Witch", "Food_Wildmeat_Raw", (String)null, (String)null),
   SCARAK_LOUSE(AnimalType.Category.SCARAK, "Scarak_Louse", "Food_Wildmeat_Raw", (String)null, (String)null),
   SCARAK_SEEKER(AnimalType.Category.SCARAK, "Scarak_Seeker", "Food_Wildmeat_Raw", (String)null, (String)null),
   SCARAK_FIGHTER(AnimalType.Category.SCARAK, "Scarak_Fighter", "Food_Wildmeat_Raw", (String)null, (String)null),
   SCARAK_DEFENDER(AnimalType.Category.SCARAK, "Scarak_Defender", "Food_Wildmeat_Raw", (String)null, (String)null),
   SCARAK_BROODMOTHER(AnimalType.Category.SCARAK, "Scarak_Broodmother", "Food_Wildmeat_Raw", (String)null, (String)null),
   KWEEBEC_SEEDLING(AnimalType.Category.KWEEBEC, "Kweebec_Seedling", "Plant_Fruit_Apple", (String)null, (String)null),
   KWEEBEC_SPROUTLING(AnimalType.Category.KWEEBEC, "Kweebec_Sproutling", "Plant_Fruit_Apple", (String)null, (String)null),
   KWEEBEC_SAPLING(AnimalType.Category.KWEEBEC, "Kweebec_Sapling", "Plant_Fruit_Apple", (String)null, (String)null),
   KWEEBEC_ROOTLING(AnimalType.Category.KWEEBEC, "Kweebec_Rootling", "Plant_Fruit_Apple", (String)null, (String)null),
   KWEEBEC_RAZORLEAF(AnimalType.Category.KWEEBEC, "Kweebec_Razorleaf", "Plant_Fruit_Apple", (String)null, (String)null),
   KWEEBEC_ELDER(AnimalType.Category.KWEEBEC, "Kweebec_Elder", "Plant_Fruit_Apple", (String)null, (String)null),
   OUTLANDER_PEON(AnimalType.Category.OUTLANDER, "Outlander_Peon", "Food_Bread", (String)null, (String)null),
   OUTLANDER_HUNTER(AnimalType.Category.OUTLANDER, "Outlander_Hunter", "Food_Bread", (String)null, (String)null),
   OUTLANDER_MARAUDER(AnimalType.Category.OUTLANDER, "Outlander_Marauder", "Food_Bread", (String)null, (String)null),
   OUTLANDER_STALKER(AnimalType.Category.OUTLANDER, "Outlander_Stalker", "Food_Bread", (String)null, (String)null),
   OUTLANDER_BERSERKER(AnimalType.Category.OUTLANDER, "Outlander_Berserker", "Food_Bread", (String)null, (String)null),
   OUTLANDER_BRUTE(AnimalType.Category.OUTLANDER, "Outlander_Brute", "Food_Bread", (String)null, (String)null),
   OUTLANDER_CULTIST(AnimalType.Category.OUTLANDER, "Outlander_Cultist", "Food_Bread", (String)null, (String)null),
   OUTLANDER_PRIEST(AnimalType.Category.OUTLANDER, "Outlander_Priest", "Food_Bread", (String)null, (String)null),
   OUTLANDER_SORCERER(AnimalType.Category.OUTLANDER, "Outlander_Sorcerer", "Food_Bread", (String)null, (String)null),
   CRAWLER_VOID(AnimalType.Category.VOID, "Crawler_Void", "Ingredient_Void_Essence", (String)null, (String)null),
   EYE_VOID(AnimalType.Category.VOID, "Eye_Void", "Ingredient_Void_Essence", (String)null, (String)null),
   LARVA_VOID(AnimalType.Category.VOID, "Larva_Void", "Ingredient_Void_Essence", (String)null, (String)null),
   SPAWN_VOID(AnimalType.Category.VOID, "Spawn_Void", "Ingredient_Void_Essence", (String)null, (String)null),
   SPECTRE_VOID(AnimalType.Category.VOID, "Spectre_Void", "Ingredient_Void_Essence", (String)null, (String)null),
   HEDERA(AnimalType.Category.MISC, "Hedera", "Plant_Fruit_Berries_Red", (String)null, (String)null);

   private final Category category;
   private final String modelAssetId;
   private final String breedingFood;
   private final String babyModelAssetId;
   private final String babyNpcRoleId;

   private AnimalType(Category category, String modelAssetId, String breedingFood, String babyModelAssetId, String babyNpcRoleId) {
      this.category = category;
      this.modelAssetId = modelAssetId;
      this.breedingFood = breedingFood;
      this.babyModelAssetId = babyModelAssetId;
      this.babyNpcRoleId = babyNpcRoleId;
   }

   public Category getCategory() {
      return this.category;
   }

   public boolean isLivestock() {
      return this.category == AnimalType.Category.LIVESTOCK;
   }

   public boolean usesVanillaTaming() {
      return this.category == AnimalType.Category.LIVESTOCK;
   }

   public String getVanillaTamedRoleName() {
      return "Tamed_" + this.modelAssetId;
   }

   public String getVanillaTamedBabyRoleName() {
      return this.babyNpcRoleId == null ? null : "Tamed_" + this.babyNpcRoleId;
   }

   public String getTamedNpcRolePath() {
      return "NPC/Roles/Creature/Livestock/Tamed/Tamed_" + this.modelAssetId;
   }

   public String getTamedBabyNpcRolePath() {
      return this.babyNpcRoleId == null ? null : "NPC/Roles/Creature/Livestock/Tamed/Tamed_" + this.babyNpcRoleId;
   }

   public boolean isMountable() {
      switch (this.ordinal()) {
         case 5:
         case 6:
         case 7:
            return true;
         case 11:
         case 22:
         case 23:
         case 24:
         case 25:
         case 27:
         case 54:
            return false;
         default:
            return false;
      }
   }

   public String getModelAssetId() {
      return this.modelAssetId;
   }

   public String getId() {
      return this.modelAssetId.toLowerCase();
   }

   public String getDefaultBreedingFood() {
      return this.breedingFood;
   }

   public String getBreedingFood() {
      return this.breedingFood;
   }

   public String getBabyModelAssetId() {
      return this.babyModelAssetId;
   }

   public String getBabyNpcRoleId() {
      return this.babyNpcRoleId;
   }

   public String getAdultNpcRoleId() {
      return this.modelAssetId;
   }

   public String getNpcRolePath() {
      switch (this.ordinal()) {
         case 37:
         case 38:
            return "NPC/Roles/Avian/Fowl/" + this.modelAssetId;
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 55:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 64:
         case 65:
         case 66:
         case 67:
         case 68:
         case 69:
         case 70:
         case 71:
         case 78:
         case 79:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         case 85:
         case 86:
         case 87:
         case 102:
         case 103:
         case 104:
         case 105:
         case 106:
         case 107:
         case 108:
         case 109:
         case 110:
         case 111:
         case 114:
         case 115:
         default:
            String prefix = this.category.getNpcRolePathPrefix();
            if (prefix == null) {
               return null;
            }

            return prefix + "/" + this.modelAssetId;
         case 52:
         case 53:
         case 54:
         case 112:
         case 113:
            return "NPC/Roles/Avian/Raptor/" + this.modelAssetId;
         case 72:
         case 73:
         case 74:
         case 75:
         case 76:
         case 77:
            return "NPC/Roles/Aquatic/Abyssal/" + this.modelAssetId;
         case 88:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         case 97:
         case 98:
         case 99:
         case 100:
         case 101:
            return "NPC/Roles/Aquatic/Marine/" + this.modelAssetId;
         case 116:
            return "NPC/Roles/Undead/Skeleton/" + this.modelAssetId;
         case 117:
         case 118:
         case 119:
         case 120:
         case 121:
            return "NPC/Roles/Undead/Zombie/" + this.modelAssetId;
         case 122:
         case 123:
         case 124:
         case 125:
         case 126:
         case 127:
         case 128:
         case 129:
         case 130:
            return "NPC/Roles/Undead/" + this.modelAssetId;
      }
   }

   public boolean hasBabyVariant() {
      return this.babyNpcRoleId != null;
   }

   public float getScaleForStage(GrowthStage stage) {
      switch (stage) {
         case BABY:
            return 0.4F;
         case JUVENILE:
            return 0.7F;
         case ADULT:
         default:
            return 1.0F;
      }
   }

   public static AnimalType fromModelAssetId(String modelAssetId) {
      if (modelAssetId == null) {
         return null;
      } else {
         for(AnimalType type : values()) {
            if (type.modelAssetId.equalsIgnoreCase(modelAssetId)) {
               return type;
            }

            if (type.babyModelAssetId != null && type.babyModelAssetId.equalsIgnoreCase(modelAssetId)) {
               return type;
            }
         }

         return null;
      }
   }

   public static boolean isBabyVariant(String identifier) {
      if (identifier == null) {
         return false;
      } else {
         for(AnimalType type : values()) {
            if (type.babyModelAssetId != null && type.babyModelAssetId.equalsIgnoreCase(identifier)) {
               return true;
            }

            if (type.babyNpcRoleId != null && type.babyNpcRoleId.equalsIgnoreCase(identifier)) {
               return true;
            }
         }

         return false;
      }
   }

   public static AnimalType fromEntityTypeId(String entityTypeId) {
      if (entityTypeId == null) {
         return null;
      } else {
         AnimalType exact = fromModelAssetId(entityTypeId);
         if (exact != null) {
            return exact;
         } else {
            String lowerId = entityTypeId.toLowerCase();

            for(AnimalType type : values()) {
               if (lowerId.contains(type.modelAssetId.toLowerCase())) {
                  return type;
               }
            }

            return null;
         }
      }
   }

   public boolean isBreedingFood(String itemId) {
      return itemId == null ? false : itemId.equalsIgnoreCase(this.breedingFood);
   }

   // $FF: synthetic method
   private static AnimalType[] $values() {
      return new AnimalType[]{COW, PIG, CHICKEN, SHEEP, GOAT, HORSE, CAMEL, RAM, TURKEY, BOAR, RABBIT, BISON, CHICKEN_DESERT, MOUFLON, PIG_WILD, SKRILL, WARTHOG, WOLF, WOLF_WHITE, FOX, BEAR_GRIZZLY, BEAR_POLAR, DEER_DOE, DEER_STAG, MOOSE_BULL, MOOSE_COW, HYENA, ANTELOPE, ARMADILLO, LEOPARD_SNOW, MOSSHORN, TIGER_SABERTOOTH, FROG, GECKO, MEERKAT, MOUSE, SQUIRREL, DUCK, PIGEON, BAT, BAT_ICE, BLUEBIRD, CROW, FINCH_GREEN, FLAMINGO, OWL_BROWN, OWL_SNOW, PARROT, PENGUIN, RAVEN, SPARROW, WOODPECKER, HAWK, VULTURE, TETRABIRD, TORTOISE, CROCODILE, LIZARD_SAND, TOAD_RHINO, TOAD_RHINO_MAGMA, RAT, MOLERAT, LARVA_SILK, SCORPION, SLUG_MAGMA, SNAIL_FROST, SNAIL_MAGMA, SNAKE_COBRA, SNAKE_MARSH, SNAKE_RATTLE, SPIDER, SPIDER_CAVE, EEL_MORAY, SHARK_HAMMERHEAD, SHELLFISH_LAVA, TRILOBITE, TRILOBITE_BLACK, WHALE_HUMPBACK, BLUEGILL, CATFISH, FROSTGILL, MINNOW, PIKE, PIRANHA, PIRANHA_BLACK, SALMON, SNAPJAW, TROUT_RAINBOW, CLOWNFISH, CRAB, JELLYFISH_BLUE, JELLYFISH_CYAN, JELLYFISH_GREEN, JELLYFISH_MAN_OF_WAR, JELLYFISH_RED, JELLYFISH_YELLOW, LOBSTER, PUFFERFISH, TANG_BLUE, TANG_CHEVRON, TANG_LEMON_PEEL, TANG_SAILFIN, EMBERWULF, YETI, FEN_STALKER, CACTEE, HATWORM, SNAPDRAGON, SPARK_LIVING, TRILLODON, RAPTOR_CAVE, REX_CAVE, ARCHAEOPTERYX, PTERODACTYL, DRAGON_FIRE, DRAGON_FROST, SKELETON, ZOMBIE, ZOMBIE_BURNT, ZOMBIE_FROST, ZOMBIE_SAND, ZOMBIE_ABERRANT, GHOUL, WRAITH, WEREWOLF, SHADOW_KNIGHT, HORSE_SKELETON, HOUND_BLEACHED, CHICKEN_UNDEAD, COW_UNDEAD, PIG_UNDEAD, GOLEM_CRYSTAL_EARTH, GOLEM_CRYSTAL_FLAME, GOLEM_CRYSTAL_FROST, GOLEM_CRYSTAL_SAND, GOLEM_CRYSTAL_THUNDER, GOLEM_FIRESTEEL, SPIRIT_EMBER, SPIRIT_FROST, SPIRIT_ROOT, SPIRIT_THUNDER, GOBLIN_SCRAPPER, GOBLIN_THIEF, GOBLIN_MINER, GOBLIN_LOBBER, GOBLIN_SCAVENGER, GOBLIN_HERMIT, GOBLIN_OGRE, GOBLIN_DUKE, TRORK_BRAWLER, TRORK_WARRIOR, TRORK_HUNTER, TRORK_SENTRY, TRORK_GUARD, TRORK_MAULER, TRORK_SHAMAN, TRORK_CHIEFTAIN, TRORK_DOCTOR_WITCH, SCARAK_LOUSE, SCARAK_SEEKER, SCARAK_FIGHTER, SCARAK_DEFENDER, SCARAK_BROODMOTHER, KWEEBEC_SEEDLING, KWEEBEC_SPROUTLING, KWEEBEC_SAPLING, KWEEBEC_ROOTLING, KWEEBEC_RAZORLEAF, KWEEBEC_ELDER, OUTLANDER_PEON, OUTLANDER_HUNTER, OUTLANDER_MARAUDER, OUTLANDER_STALKER, OUTLANDER_BERSERKER, OUTLANDER_BRUTE, OUTLANDER_CULTIST, OUTLANDER_PRIEST, OUTLANDER_SORCERER, CRAWLER_VOID, EYE_VOID, LARVA_VOID, SPAWN_VOID, SPECTRE_VOID, HEDERA};
   }

   public static enum Category {
      LIVESTOCK,
      MAMMAL,
      CRITTER,
      AVIAN,
      REPTILE,
      VERMIN,
      AQUATIC,
      MYTHIC,
      DINOSAUR,
      BOSS,
      UNDEAD,
      GOLEM,
      SPIRIT,
      GOBLIN,
      TRORK,
      SCARAK,
      KWEEBEC,
      OUTLANDER,
      VOID,
      MISC;

      public String getNpcRolePathPrefix() {
         switch (this.ordinal()) {
            case 0:
               return "NPC/Roles/Creature/Livestock";
            case 1:
               return "NPC/Roles/Creature/Mammal";
            case 2:
               return "NPC/Roles/Creature/Critter";
            case 3:
               return "NPC/Roles/Avian/Aerial";
            case 4:
               return "NPC/Roles/Creature/Reptile";
            case 5:
               return "NPC/Roles/Creature/Vermin";
            case 6:
               return "NPC/Roles/Aquatic/Freshwater";
            case 7:
               return "NPC/Roles/Creature/Mythic";
            case 8:
               return "NPC/Roles/Creature/Reptile";
            case 9:
               return "NPC/Roles/Boss";
            case 10:
               return "NPC/Roles/Undead";
            case 11:
               return "NPC/Roles/Elemental/Golem";
            case 12:
               return "NPC/Roles/Elemental/Spirit";
            case 13:
               return "NPC/Roles/Intelligent/Aggressive/Goblin";
            case 14:
               return "NPC/Roles/Intelligent/Aggressive/Trork";
            case 15:
               return "NPC/Roles/Intelligent/Aggressive/Scarak";
            case 16:
               return "NPC/Roles/Intelligent/Neutral/Kweebec";
            case 17:
               return "NPC/Roles/Intelligent/Aggressive/Outlander";
            case 18:
               return "NPC/Roles/Void";
            case 19:
            default:
               return null;
         }
      }

      // $FF: synthetic method
      private static Category[] $values() {
         return new Category[]{LIVESTOCK, MAMMAL, CRITTER, AVIAN, REPTILE, VERMIN, AQUATIC, MYTHIC, DINOSAUR, BOSS, UNDEAD, GOLEM, SPIRIT, GOBLIN, TRORK, SCARAK, KWEEBEC, OUTLANDER, VOID, MISC};
      }
   }
}
