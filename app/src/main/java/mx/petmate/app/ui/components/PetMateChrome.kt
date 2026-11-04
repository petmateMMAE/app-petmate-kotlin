package mx.petmate.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.petmate.app.R
import mx.petmate.app.ui.theme.petMateColors

/** Logo de PetMate (placa amarilla con huella). */
@Composable
fun PetMateLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_pm_logo),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.size(size),
    )
}

/** Estilo del boton de icono cuadrado (.icon-btn, .plain y .help de la maqueta). */
enum class IconButtonStyle { Raised, Plain, Help }

/**
 * Boton cuadrado con icono. La maqueta lo dibuja de 44 px; aqui mide 48 dp
 * para cumplir el area tactil minima.
 */
@Composable
fun PetMateIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Raised,
    tint: Color? = null,
) {
    val extra = MaterialTheme.petMateColors
    val shape = RoundedCornerShape(14.dp)
    val (container, content) = when (style) {
        IconButtonStyle.Raised -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        IconButtonStyle.Plain -> Color.Transparent to MaterialTheme.colorScheme.onSurface
        IconButtonStyle.Help -> extra.sunSoft to extra.sunInk
    }
    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = tint ?: content,
        modifier = modifier
            .then(if (style == IconButtonStyle.Raised) Modifier.shadow(4.dp, shape) else Modifier)
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Barra superior con boton de regresar (la de login y registro). */
@Composable
fun PetMateBackBar(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    ) {
        PetMateIconButton(
            icon = R.drawable.ic_pm_back,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBackClick,
        )
    }
}

/** Pestañas de la barra inferior del dueño de mascota. */
enum class OwnerTab(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    Pets(R.string.tab_pets, R.drawable.ic_pm_paw),
    Map(R.string.tab_map, R.drawable.ic_pm_pin),
    Reminders(R.string.tab_reminders, R.drawable.ic_pm_bell),
    Profile(R.string.tab_profile, R.drawable.ic_pm_user),
}

/**
 * Barra inferior de la maqueta: cuatro pestañas, la activa con pastilla
 * morada suave y "Avisos" con contador rojo.
 */
@Composable
fun PetMateBottomBar(
    selected: OwnerTab,
    onTabSelected: (OwnerTab) -> Unit,
    modifier: Modifier = Modifier,
    remindersCount: Int = 0,
) {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.petMateColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = extra.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp)
                .selectableGroup(),
        ) {
            OwnerTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) extra.brandText else extra.ink3
                val label = stringResource(tab.label)
                val badgeDescription = if (tab == OwnerTab.Reminders && remindersCount > 0) {
                    pluralStringResource(R.plurals.tab_reminders_badge, remindersCount, remindersCount)
                } else {
                    null
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { onTabSelected(tab) },
                            role = Role.Tab,
                        )
                        .semantics {
                            contentDescription = listOfNotNull(label, badgeDescription).joinToString(", ")
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(58.dp)
                            .height(32.dp)
                            .background(
                                color = if (isSelected) extra.brandSoft else Color.Transparent,
                                shape = CircleShape,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                        if (badgeDescription != null) {
                            CountBadge(
                                count = remindersCount,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = 11.dp, y = (-9).dp),
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                    )
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val extra = MaterialTheme.petMateColors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
            .background(extra.alert, CircleShape)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onError,
            textAlign = TextAlign.Center,
        )
    }
}

/** Etiqueta redonda chica (.badge de la maqueta). */
@Composable
fun PetMateBadge(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(container, CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}
