package id.ac.pnm.yourtexttoanonymous.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSetupScreen(
    inputName: String,
    inputGender: String,
    onNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Text("Complete Your Profile", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Set up your identity before meeting strangers.",
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    TextField(
        value = inputName,
        onValueChange = onNameChange,
        label = { Text("Display Name / Username") },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        singleLine = true
    )

    Text("Gender:")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
        RadioButton(selected = inputGender == "Male", onClick = { onGenderChange("Male") })
        Text("Male")
        Spacer(modifier = Modifier.width(16.dp))
        RadioButton(selected = inputGender == "Female", onClick = { onGenderChange("Female") })
        Text("Female")
    }

    Button(
        onClick = onSaveClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save Profile & Continue")
    }
}