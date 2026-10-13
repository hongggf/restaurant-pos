-- Add allow_notes column to categories table
ALTER TABLE categories ADD COLUMN IF NOT EXISTS allow_notes BOOLEAN NOT NULL DEFAULT TRUE;

-- Turn off notes by default for pre-packaged / canned categories
UPDATE categories SET allow_notes = FALSE WHERE name_en IN ('I Love Soft Drinks', 'I Love Candy');
