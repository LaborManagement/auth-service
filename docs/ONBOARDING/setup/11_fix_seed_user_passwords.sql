-- Generated bcrypt password hashes (strength 10) for all 7 seed users
-- Each password is unique as bcrypt generates different salts each time

SET search_path TO auth;

UPDATE users SET password = '$2a$10$OyCA1lgOZBk/qb8dahYeD.UrSyuY2urN2eC1IHYgOzuSOaBhBoD4u' WHERE username = 'test.user';
UPDATE users SET password = '$2a$10$m2WyB7vO.wPiScT1Dm.gw.64oC2Eq4lOzEy798QR0ktIg4jdBgaT.' WHERE username = 'admin.tech';
UPDATE users SET password = '$2a$10$aTm0BsUSwGerSrNrOau8..lV8V7Z5VQO0mnaa1txPLLLgX3A8xRbW' WHERE username = 'worker1';
UPDATE users SET password = '$2a$10$K7ZdK4muqK0KrXwcTwV/auNw6C8AF0VYPtrUlB8IGIeByIWFJ1ie6' WHERE username = 'platform.bootstrap';
UPDATE users SET password = '$2a$10$RsCl4r3u43sTIToUOsjKyOKYh.lmix.hWMCAhRgwDHVdPratagQRW' WHERE username = 'admin.ops';
UPDATE users SET password = '$2a$10$97nkDtl5VXGUWs6jd7XOV.gjtGe3bwS/NgDFuykUZka/2kWTjWxbC' WHERE username = 'board1';
UPDATE users SET password = '$2a$10$zFVoBF9dPxD01NoD6srsIeU4I2fHeQxJBzZxlMVPc9ZKIqFJ1FSyG' WHERE username = 'employer1';

-- Verify the update
SELECT username, password FROM users WHERE username IN ('platform.bootstrap', 'admin.tech', 'admin.ops', 'board1', 'employer1', 'worker1', 'test.user') ORDER BY username;
