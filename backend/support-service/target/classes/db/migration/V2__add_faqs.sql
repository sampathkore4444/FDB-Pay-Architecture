CREATE TABLE faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL'
);

INSERT INTO faqs (question, answer, sort_order, category) VALUES
('How do I reset my PIN?', 'Go to Profile > Security > Reset PIN and follow the instructions.', 1, 'SECURITY'),
('How long do transfers take?', 'Transfers are typically processed within seconds. International remittances may take 1-2 business days.', 2, 'PAYMENT'),
('What are the transaction limits?', 'Daily limit is 500,000 MMK. Monthly limit is 5,000,000 MMK. Limits may vary based on KYC tier.', 3, 'PAYMENT'),
('How do I deposit money?', 'Visit any authorized agent location with your ID to perform a cash-in transaction.', 4, 'GENERAL'),
('How do I create an account?', 'Download the app and register using your phone number and a valid ID.', 5, 'ACCOUNT'),
('How do I update my profile information?', 'Go to Profile > Edit Profile to update your personal information.', 6, 'ACCOUNT'),
('What should I do if I forget my password?', 'Click "Forgot Password" on the login screen and follow the instructions sent to your registered email.', 7, 'SECURITY'),
('How do I contact customer support?', 'Create a support ticket from the app or call our hotline at 01-234-5678.', 8, 'GENERAL');
