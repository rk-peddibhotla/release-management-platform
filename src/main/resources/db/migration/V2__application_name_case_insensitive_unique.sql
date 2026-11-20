CREATE UNIQUE INDEX ux_applications_name_lower
    ON applications (LOWER(name));