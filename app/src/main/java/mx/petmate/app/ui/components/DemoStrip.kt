package mx.petmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.petmate.app.R
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/**
 * Franja amarilla del modo demo (.demo-strip de la maqueta): se ve arriba de cada pantalla
 * mientras el usuario usa "Probar sin cuenta" y le recuerda que los datos son de ejemplo.
 */
@Composable
fun DemoStrip(onCreateAccountClick: () -> Unit, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 6.dp)
            .background(extra.sun, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
    ) {
        Text(
            text = stringResource(R.string.demo_tag),
            color = extra.sun,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .background(extra.onSun, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
        Text(
            text = stringResource(R.string.demo_sample_data),
            color = extra.onSun,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 32.dp)
                .background(extra.onSun, CircleShape)
                .clickable(onClick = onCreateAccountClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.demo_create_account),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoStripPreview() {
    PetMateEdelTheme { DemoStrip(onCreateAccountClick = {}) }
}
