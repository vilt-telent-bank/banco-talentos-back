CREATE UNIQUE INDEX IF NOT EXISTS uk_profiles_cpf ON profiles (cpf) WHERE cpf IS NOT NULL;
