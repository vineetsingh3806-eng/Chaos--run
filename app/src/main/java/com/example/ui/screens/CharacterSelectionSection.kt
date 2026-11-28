package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AaravState
import com.example.data.PlayableCharacter
import com.example.engine.PlayerCharacterRenderer

/**
 * Dedicated Character Selection section rendered exclusively inside SettingsModal.
 * Displays all four original cartoon runners with live vector previews,
 * names, short personality descriptions, and explicit Select / Selected states.
 */
@Composable
fun CharacterSelectionSection(
    selectedCharacter: PlayableCharacter,
    onSelectCharacter: (PlayableCharacter) -> Unit,
    modifier: Modifier = Modifier
) {
  val characterRenderer = remember { PlayerCharacterRenderer() }
  val characters = PlayableCharacter.entries
  val topRow = characters.take(2)
  val bottomRow = characters.drop(2)

  Column(
      modifier = modifier.testTag("section_character_selection"),
      verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
          text = "CHOOSE YOUR RUNNER",
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.8.sp,
          color = Color(0xFFFDE047)
      )
      Text(
          text = "Active: ${selectedCharacter.displayName}",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          color = Color(selectedCharacter.accentColorHex)
      )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      for (character in topRow) {
        CharacterOptionCard(
            character = character,
            isSelected = character == selectedCharacter,
            renderer = characterRenderer,
            onSelect = { onSelectCharacter(character) },
            modifier = Modifier.weight(1f)
        )
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      for (character in bottomRow) {
        CharacterOptionCard(
            character = character,
            isSelected = character == selectedCharacter,
            renderer = characterRenderer,
            onSelect = { onSelectCharacter(character) },
            modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun CharacterOptionCard(
    character: PlayableCharacter,
    isSelected: Boolean,
    renderer: PlayerCharacterRenderer,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
  val accent = Color(character.accentColorHex)
  val borderColor = if (isSelected) accent else Color(0xFF283D47)
  val cardBg = if (isSelected) accent.copy(alpha = 0.14f) else Color(0xFF091318)

  Surface(
      shape = RoundedCornerShape(13.dp),
      color = cardBg,
      border = BorderStroke(if (isSelected) 2.dp else 1.2.dp, borderColor),
      modifier = modifier
          .heightIn(min = 88.dp)
          .clip(RoundedCornerShape(13.dp))
          .semantics { selected = isSelected }
          .clickable(onClick = onSelect)
          .testTag("character_card_${character.id}")
  ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
      // Real vector preview rendered via PlayerCharacterRenderer
      Box(
          modifier = Modifier
              .size(width = 44.dp, height = 56.dp)
              .clip(RoundedCornerShape(9.dp))
              .background(Color(0xFF0F2129))
              .border(1.dp, accent.copy(alpha = if (isSelected) 0.9f else 0.4f), RoundedCornerShape(9.dp))
              .semantics(mergeDescendants = true) {}
              .testTag("character_preview_${character.id}"),
          contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val previewState = if (isSelected) AaravState.VICTORY else AaravState.IDLE
          drawIntoCanvas { canvas ->
            renderer.drawPreview(
                canvas = canvas.nativeCanvas,
                character = character,
                width = size.width,
                height = size.height,
                animTick = 8,
                state = previewState
            )
          }
        }
      }

      Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
              text = character.displayName,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFFBEB),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
          )

          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
              modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) accent else Color(0xFF1E293B))
                  .padding(horizontal = 5.dp, vertical = 2.dp)
                  .testTag("character_status_${character.id}")
          ) {
            if (isSelected) {
              Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color(0xFF091318),
                  modifier = Modifier.size(9.dp)
              )
            }
            Text(
                text = if (isSelected) "SELECTED" else "SELECT",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color(0xFF091318) else Color(0xFFE2E8F0)
            )
          }
        }

        Text(
            text = character.subtitle,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = character.description,
            fontSize = 9.5.sp,
            lineHeight = 11.5.sp,
            color = Color(0xFFCBD5E1),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
