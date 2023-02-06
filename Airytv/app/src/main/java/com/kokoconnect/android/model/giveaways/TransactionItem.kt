package com.kokoconnect.android.model.giveaways

import com.kokoconnect.android.model.profile.Profile

abstract class TransactionItem(val type: TransactionItemType) {}

class TransactionItemData(var transaction: Transaction): TransactionItem(TransactionItemType.TRANSACTION_DATA)

class TransactionItemHeader(var profile: Profile): TransactionItem(TransactionItemType.TRANSACTION_HEADER)

enum class TransactionItemType() {
    TRANSACTION_DATA,
    TRANSACTION_HEADER
}