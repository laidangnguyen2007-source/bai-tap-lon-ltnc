# Layout Bug Fix: Unbreakable Text Expansion

**Issue:**
When a user input (such as the product description) contained a very long continuous string without any spaces, the `Label` component with `wrapText="true"` failed to wrap the text. Instead, it forced its parent `GridPane` column to expand infinitely. This expansion pushed the adjacent "Auction Information" panel to the right, squishing it and ruining the UI layout.

**Resolution:**
1. **Right Panel Constraint:** Added strict width constraints (`minWidth="320" maxWidth="320"`) to the "Auction Information" `VBox` to ensure it never shrinks or gets squished by adjacent expanding panels.
2. **Label Shrink Configuration:** Added `minWidth="0"` to the `Label` elements in the `GridPane` (itemNameLabel, itemCategoryLabel, itemDescriptionLabel). In JavaFX, this allows the layout manager to shrink the label smaller than its preferred width (the length of the unbroken word). When the column runs out of available space, the unbreakable text is gracefully truncated with an ellipsis (`...`) instead of breaking the layout.

**Modified Files:**
- `auction-client/src/main/resources/com/auction/client/fxml/auction-detail.fxml`
