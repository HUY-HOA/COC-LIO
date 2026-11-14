# Gmail build-result notifications

The APK workflow sends one short email after the build finishes:

- Successful build: subject and body are `BUILD APK THÀNH CÔNG`.
- Failed build: subject and body are `BUILD APK THẤT BẠI`.

The workflow does not post success/failure comments to GitHub.

## One-time Gmail setup

1. Enable 2-Step Verification on the Gmail account used by the workflow.
2. Create a Google App Password for the workflow. Do not use the normal Gmail password.
3. In the GitHub repository, open **Settings → Secrets and variables → Actions**.
4. Add a repository secret named `GMAIL_APP_PASSWORD` with the generated App Password.
5. Do not commit the password to this repository or send it in chat.

If the secret is missing or SMTP delivery fails, the APK build itself is not marked failed because of the email step. The workflow log will contain a warning instead.
