with open('app/src/main/java/com/example/ui/screens/StudentDashboardScreen.kt', 'r') as f:
    content = f.read()

marker = 'download_receipt_live_'
idx = content.find(marker)
if idx == -1:
    print('ERROR: download_receipt_live_ not found!')
else:
    # Look back for "else if (order.status.uppercase() == \"COMPLETED\")"
    start_tag = 'else if (order.status.uppercase() == "COMPLETED")'
    start_idx = content.rfind(start_tag, 0, idx)
    if start_idx == -1:
         print('ERROR: start_tag not found before marker')
    else:
         # Find the indent
         line_start = content.rfind('\n', 0, start_idx) + 1
         indent = content[line_start:start_idx]
         
         receipt_idx = content.find('Download PDF Receipt', idx)
         if receipt_idx == -1:
              print('ERROR: Download PDF Receipt text not found')
         else:
              close_1 = content.find('}', receipt_idx)
              close_2 = content.find('}', close_1 + 1)
              end_idx = close_2 + 1
              
              print('Found range to replace! Replacing...')
              
              replacement = """                     } else if (order.status.uppercase() == "COMPLETED" || order.status.uppercase() == "DELIVERED") {
                                                         Text(
                                                             text = "ORDER SAFELY FULFILLED",
                                                             fontSize = 10.sp,
                                                             fontWeight = FontWeight.Bold,
                                                             color = MaterialTheme.colorScheme.primary
                                                         )
                                                         Text(
                                                             text = "Successfully retrieved custody of your meal!",
                                                             fontWeight = FontWeight.ExtraBold,
                                                             fontSize = 12.sp,
                                                             color = MaterialTheme.colorScheme.primary
                                                         )
                                                         Spacer(modifier = Modifier.height(4.dp))
                                                         Row(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             val context = androidx.compose.ui.platform.LocalContext.current
                                                             OutlinedButton(
                                                                 onClick = { generatePdfReceipt(context, order) },
                                                                 modifier = Modifier.padding(top = 4.dp).testTag("download_receipt_live_${order.id}").height(34.dp),
                                                                 colors = ButtonDefaults.outlinedButtonColors(
                                                                     contentColor = MaterialTheme.colorScheme.primary
                                                                 ),
                                                                 shape = RoundedCornerShape(8.dp),
                                                                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                                                 contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                             ) {
                                                                 Icon(
                                                                     imageVector = Icons.Default.Print,
                                                                     contentDescription = "PDF Receipt",
                                                                     modifier = Modifier.size(12.dp)
                                                                 )
                                                                 Spacer(modifier = Modifier.width(4.dp))
                                                                 Text("Download Receipt", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                             }

                                                             if (orderFeedback == null) {
                                                                 Button(
                                                                     onClick = { feedbackTargetOrder = order },
                                                                     modifier = Modifier.padding(top = 4.dp).testTag("feedback_button_live_${order.id}").height(34.dp),
                                                                     colors = ButtonDefaults.buttonColors(
                                                                         containerColor = MaterialTheme.colorScheme.secondary,
                                                                         contentColor = MaterialTheme.colorScheme.onSecondary
                                                                     ),
                                                                     shape = RoundedCornerShape(8.dp),
                                                                     contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                 ) {
                                                                     Icon(
                                                                         imageVector = Icons.Default.Star,
                                                                         contentDescription = null,
                                                                         modifier = Modifier.size(12.dp)
                                                                     )
                                                                     Spacer(modifier = Modifier.width(4.dp))
                                                                     Text("Rate Vendor", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                                 }
                                                             } else {
                                                                 val avgRating = (orderFeedback.ratingFoodQuality + orderFeedback.ratingCleanliness + orderFeedback.ratingServiceSpeed + orderFeedback.ratingPriceValue) / 4.0
                                                                 Row(
                                                                     verticalAlignment = Alignment.CenterVertically,
                                                                     modifier = Modifier.padding(top = 4.dp)
                                                                 ) {
                                                                     Icon(
                                                                         imageVector = Icons.Default.Star,
                                                                         contentDescription = null,
                                                                         tint = Color(0xFFF9A825),
                                                                         modifier = Modifier.size(14.dp)
                                                                     )
                                                                     Spacer(modifier = Modifier.width(4.dp))
                                                                     Text(
                                                                         text = "%.1f ★".format(avgRating),
                                                                         fontSize = 11.sp,
                                                                         fontWeight = FontWeight.Bold,
                                                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                     )
                                                                 }
                                                             }
                                                         }
                                                     }"""
              
              new_content = content[:line_start] + replacement + content[end_idx:]
              with open('app/src/main/java/com/example/ui/screens/StudentDashboardScreen.kt', 'w') as f:
                  f.write(new_content)
              print('SUCCESSFULLY REPLACED!')
