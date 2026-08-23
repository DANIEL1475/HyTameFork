package com.hytame.util;

import com.hytame.models.AnimalType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AnimalNameGenerator {
   private static final Random random = new Random();
   public static final int MAX_NAME_LENGTH = 48;
   private static final List<String> GENERIC_NAMES = Arrays.asList("Buddy", "Max", "Bella", "Charlie", "Luna", "Milo", "Coco", "Rocky", "Daisy", "Duke", "Sadie", "Bear", "Molly", "Tucker", "Bailey", "Maggie", "Jack", "Sophie", "Oliver", "Lucy", "Buster", "Chloe", "Teddy", "Penny", "Zeus", "Zoey", "Gus", "Lily", "Winston", "Gracie", "Oscar", "Ruby");
   private static final List<String> FARM_NAMES = Arrays.asList("Bessie", "Clover", "Buttercup", "Daisy", "Patches", "Spot", "Brownie", "Cookie", "Ginger", "Honey", "Maple", "Mocha", "Pepper", "Sugar");
   private static final List<String> COW_NAMES = Arrays.asList("Bessie", "Buttercup", "Daisy", "Milkshake", "Moo-Moo", "Clarabelle", "Bovina", "Cowbell", "Cream Puff", "Patches", "Spot", "Brownie");
   private static final List<String> PIG_NAMES = Arrays.asList("Hamlet", "Bacon", "Wilbur", "Piglet", "Truffles", "Porky", "Snuffles", "Oink", "Muddy", "Babe", "Pinky", "Waddles", "Hammy", "Curly");
   private static final List<String> CHICKEN_NAMES = Arrays.asList("Clucky", "Henny", "Pecky", "Nugget", "Feathers", "Drumstick", "Sunny", "Yolky", "Eggbert", "Chickpea", "Clucker", "Scrambles", "Omelet");
   private static final List<String> SHEEP_NAMES = Arrays.asList("Woolly", "Fluffy", "Cotton", "Cloud", "Snowball", "Lamb Chop", "Fuzzy", "Fleece", "Shearlock", "Baa-Baa", "Marshmallow", "Cashmere", "Angora");
   private static final List<String> HORSE_NAMES = Arrays.asList("Thunder", "Spirit", "Midnight", "Storm", "Blaze", "Shadow", "Star", "Lucky", "Copper", "Dusty", "Apollo", "Maverick", "Trigger", "Cinnamon");
   private static final List<String> RABBIT_NAMES = Arrays.asList("Thumper", "Cottontail", "Flopsy", "Hopscotch", "Bunbun", "Snowball", "Carrot", "Clover", "Nibbles", "Velvet", "Whiskers", "Binky", "Honey");
   private static final List<String> PREDATOR_NAMES = Arrays.asList("Fang", "Shadow", "Hunter", "Storm", "Blaze", "Thunder", "Midnight", "Raven", "Ghost", "Dusk", "Prowler", "Striker", "Fury", "Tempest");

   public static List<String> getSuggestedNames(AnimalType animalType) {
      List<String> pool = getNamePoolForType(animalType);
      List<String> shuffled = new ArrayList(pool);
      Collections.shuffle(shuffled, random);
      List<String> result = new ArrayList();

      for(int i = 0; i < Math.min(3, shuffled.size()); ++i) {
         result.add((String)shuffled.get(i));
      }

      if (result.size() < 3) {
         List<String> genericShuffled = new ArrayList(GENERIC_NAMES);
         Collections.shuffle(genericShuffled, random);

         for(String name : genericShuffled) {
            if (!result.contains(name)) {
               result.add(name);
               if (result.size() >= 3) {
                  break;
               }
            }
         }
      }

      return result;
   }

   private static List<String> getNamePoolForType(AnimalType type) {
      if (type == null) {
         return GENERIC_NAMES;
      } else {
         switch (type) {
            case COW:
               return COW_NAMES;
            case PIG:
               return PIG_NAMES;
            case CHICKEN:
               return CHICKEN_NAMES;
            case SHEEP:
               return SHEEP_NAMES;
            case HORSE:
               return HORSE_NAMES;
            case RABBIT:
               return RABBIT_NAMES;
            case WOLF:
            case FOX:
               return PREDATOR_NAMES;
            case GOAT:
            case DUCK:
            case TURKEY:
            case CAMEL:
               return FARM_NAMES;
            default:
               return GENERIC_NAMES;
         }
      }
   }

   public static String validateName(String name) {
      if (name == null) {
         return null;
      } else {
         String trimmed = name.trim();
         if (trimmed.isEmpty()) {
            return null;
         } else {
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < trimmed.length(); ++i) {
               char c = trimmed.charAt(i);
               if (c >= ' ' && (c < 127 || c > 159)) {
                  sb.append(c);
               }
            }

            String sanitized = sb.toString().trim();
            if (sanitized.isEmpty()) {
               return null;
            } else {
               if (sanitized.length() > 48) {
                  sanitized = sanitized.substring(0, 48);
               }

               return sanitized;
            }
         }
      }
   }

   public static boolean isValidName(String name) {
      return validateName(name) != null;
   }
}
