SELECT u_id,
       u_email
FROM users
WHERE u_id = $1;
