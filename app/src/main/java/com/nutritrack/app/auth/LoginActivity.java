package com.nutritrack.app.auth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.nutritrack.app.MainActivity;
import com.nutritrack.app.R;
import com.nutritrack.app.database.FirestoreManager;
import com.nutritrack.app.databinding.ActivityLoginBinding;
import com.nutritrack.app.models.User;

/**
 * Email + password sign-in/registration with Firebase Auth (SRS REQ-1, REQ-2),
 * plus Google Sign-In (UI mockup page 1).
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;
    private boolean isRegisterMode = false;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Task<GoogleSignInAccount> task =
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleResult(task);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        auth = FirebaseAuth.getInstance();

        // Google Sign-In options. The default_web_client_id string comes from
        // google-services.json once Firebase is wired up.
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            googleClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception ignored) {
            // google-services.json not yet placed; Google button will show a setup hint.
        }

        setMode(false);

        b.tabSignIn  .setOnClickListener(v -> setMode(false));
        b.tabRegister.setOnClickListener(v -> setMode(true));
        b.btnPrimary .setOnClickListener(v -> onPrimary());
        b.btnGoogle  .setOnClickListener(v -> onGoogle());
    }

    private void setMode(boolean register) {
        isRegisterMode = register;

        b.tabSignIn  .setBackground(ContextCompat.getDrawable(this,
                register ? android.R.color.transparent : R.drawable.bg_tab_selected));
        b.tabRegister.setBackground(ContextCompat.getDrawable(this,
                register ? R.drawable.bg_tab_selected : android.R.color.transparent));
        b.tabSignIn  .setTypeface(null, register ? Typeface.NORMAL : Typeface.BOLD);
        b.tabRegister.setTypeface(null, register ? Typeface.BOLD : Typeface.NORMAL);

        b.fullNameLabel       .setVisibility(register ? View.VISIBLE : View.GONE);
        b.inputFullName       .setVisibility(register ? View.VISIBLE : View.GONE);
        b.confirmPasswordLabel.setVisibility(register ? View.VISIBLE : View.GONE);
        b.inputConfirmPassword.setVisibility(register ? View.VISIBLE : View.GONE);
        b.btnPrimary.setText(register ? "Register" : "Sign In →");
        b.dividerRow.setVisibility(register ? View.GONE : View.VISIBLE);
        b.btnGoogle .setVisibility(register ? View.GONE : View.VISIBLE);
    }

    private void onPrimary() {
        String email    = b.inputEmail.getText().toString().trim();
        String password = b.inputPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            toast("Please fill in all fields");
            return;
        }

        if (isRegisterMode) {
            String fullName = b.inputFullName.getText().toString().trim();
            String confirm  = b.inputConfirmPassword.getText().toString();
            if (TextUtils.isEmpty(fullName)) {
                toast("Please enter your name");
                return;
            }
            if (!password.equals(confirm)) {
                toast("Passwords do not match");
                return;
            }
            if (password.length() < 6) {
                toast("Password must be at least 6 characters");
                return;
            }
            registerWithEmail(email, password, fullName);
        } else {
            signInWithEmail(email, password);
        }
    }

    private void signInWithEmail(String email, String password) {
        setBusy(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setBusy(false);
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        toast("Sign-in failed: " +
                                (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "unknown"));
                    }
                });
    }

    private void registerWithEmail(String email, String password, String fullName) {
        setBusy(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setBusy(false);
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        FirebaseUser fu = auth.getCurrentUser();

                        // Save the user-provided name to Firebase Auth's display name too,
                        // so it surfaces consistently even when other parts of the app fall
                        // back to FirebaseUser.getDisplayName().
                        UserProfileChangeRequest update = new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build();
                        fu.updateProfile(update);

                        User u = new User(fu.getUid(), fu.getEmail(), fullName);
                        new FirestoreManager().saveUser(u)
                                .addOnSuccessListener(unused -> goToMain())
                                .addOnFailureListener(e -> goToMain()); // proceed regardless
                    } else {
                        toast("Registration failed: " +
                                (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "unknown"));
                    }
                });
    }

    private void onGoogle() {
        if (googleClient == null) {
            toast("Add google-services.json and the default_web_client_id string to enable Google Sign-In.");
            return;
        }
        googleLauncher.launch(googleClient.getSignInIntent());
    }

    private void handleGoogleResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null) return;
            AuthCredential cred = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            auth.signInWithCredential(cred).addOnCompleteListener(this, signInTask -> {
                if (signInTask.isSuccessful() && auth.getCurrentUser() != null) {
                    FirebaseUser fu = auth.getCurrentUser();
                    User u = new User(fu.getUid(), fu.getEmail(),
                            fu.getDisplayName() != null
                                    ? fu.getDisplayName()
                                    : emailToName(fu.getEmail()));
                    new FirestoreManager().saveUser(u);
                    goToMain();
                } else {
                    toast("Google sign-in failed");
                }
            });
        } catch (ApiException e) {
            toast("Google sign-in error: " + e.getStatusCode());
        }
    }

    private String emailToName(String email) {
        if (email == null || !email.contains("@")) return "User";
        String local = email.substring(0, email.indexOf('@'));
        return Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }

    private void setBusy(boolean busy) {
        b.btnPrimary.setEnabled(!busy);
        b.btnGoogle.setEnabled(!busy);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void goToMain() {
        // Ensure the cache reflects this session's user, not any previous one.
        com.nutritrack.app.database.UserRepository.get().clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
