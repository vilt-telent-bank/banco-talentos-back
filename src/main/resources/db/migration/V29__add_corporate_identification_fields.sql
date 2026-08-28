UPDATE profiles SET registration_status = 'NOT_REQUIRED' WHERE registration_status IN ('NOT_REQUESTED', 'REJECTED');
UPDATE profiles SET registration_status = 'REQUESTED_VIA_TICKET' WHERE registration_status = 'REQUESTED';
UPDATE profiles SET registration_status = 'TICKET_AWAITING_APPROVAL' WHERE registration_status = 'AWAITING_APPROVAL';
UPDATE profiles SET registration_status = 'RELEASED' WHERE registration_status = 'APPROVED';

ALTER TABLE profiles ALTER COLUMN registration_status SET DEFAULT 'NOT_REQUIRED';

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS registration_requested_at DATE,
    ADD COLUMN IF NOT EXISTS registration_notes TEXT,
    ADD COLUMN IF NOT EXISTS has_client_machine BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS contracting_area VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cost_center VARCHAR(255),
    ADD COLUMN IF NOT EXISTS project_entry_date DATE,
    ADD COLUMN IF NOT EXISTS billable BOOLEAN,
    ADD COLUMN IF NOT EXISTS porto_onboarding BOOLEAN,
    ADD COLUMN IF NOT EXISTS project_manager_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS allocation_project_id UUID REFERENCES projects(id),
    ADD COLUMN IF NOT EXISTS allocation_squad_id UUID REFERENCES squads(id),
    ADD COLUMN IF NOT EXISTS technical_proposal_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS technical_proposal_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS technical_proposal_sent_at DATE,
    ADD COLUMN IF NOT EXISTS technical_proposal_notes TEXT,
    ADD COLUMN IF NOT EXISTS resource_status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE';

UPDATE profiles
SET resource_status = 'AVAILABLE'
WHERE registration_status IS NULL
   OR registration_status = 'NOT_REQUIRED';

UPDATE profiles
SET resource_status = 'ALLOCATED'
WHERE registration_status = 'RELEASED';

UPDATE profiles
SET resource_status = 'WAITING'
WHERE registration_status IS NOT NULL
  AND registration_status NOT IN ('NOT_REQUIRED', 'RELEASED');

CREATE TABLE IF NOT EXISTS resource_equipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    tag VARCHAR(100),
    hostname VARCHAR(255),
    asset_number VARCHAR(100),
    brand_os VARCHAR(255),
    processor VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'EMPTY',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by_id UUID,
    updated_by_id UUID
);

CREATE INDEX IF NOT EXISTS idx_resource_equipments_profile_id
    ON resource_equipments(profile_id);
