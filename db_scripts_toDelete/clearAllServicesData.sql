DELETE FROM billing.cart        WHERE TRUE;
DELETE FROM billing.movie_price WHERE TRUE;
DELETE FROM billing.sale        WHERE TRUE;
DELETE FROM billing.sale_item   WHERE TRUE;

DELETE FROM movies.movie        WHERE TRUE;
DELETE FROM movies.person       WHERE TRUE;
DELETE FROM movies.genre        WHERE TRUE;
DELETE FROM movies.movie_genre  WHERE TRUE;
DELETE FROM movies.movie_person WHERE TRUE;

DELETE FROM idm.refresh_token	WHERE TRUE;
DELETE FROM idm.role			WHERE TRUE;
DELETE FROM idm.token_status	WHERE TRUE;
DELETE FROM idm.user			WHERE TRUE;
DELETE FROM idm.user_role		WHERE TRUE;
DELETE FROM idm.user_status    	WHERE TRUE;
