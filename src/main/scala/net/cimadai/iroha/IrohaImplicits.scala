package net.cimadai.iroha

import iroha.protocol.primitive.uint256

object IrohaImplicits {

  implicit class uint256Ext(lhs: uint256) {
    private case class AddResult(carry: Int, rest: Long) {
      assert(carry == 0 || carry == 1)
    }

    private case class SubtractResult(borrow: Int, rest: Long) {
      assert(borrow == 0 || borrow == 1)
    }

    // carried is 1 or 0.
    private def isCarryOnAdd(lhsValue: Long, rhsValue: Long, carried: Int): Boolean = {
      // means `Long.MaxValue < lhsValue + rhsValue + carried`
      Long.MaxValue - lhsValue - carried < rhsValue
    }

    private def needBorrowOnSub(lhsValue: Long, rhsValue: Long, borrowed: Int): Boolean = {
      // means `lhsValue + borrowed < rhsValue`
      lhsValue < rhsValue - borrowed
    }

    private def add(lhsValue: Long, rhsValue: Long, carried: Int = 0): AddResult = {
      if (isCarryOnAdd(lhsValue, rhsValue, carried)) {
        val carry = 1
        // means: (lhsValue + rhsValue + carried) - Long.MaxValue
        val rest = rhsValue - (Long.MaxValue - lhsValue - carried)
        AddResult(carry, rest)
      } else {
        val carry = 0
        val rest = lhsValue + rhsValue + carried
        AddResult(carry, rest)
      }
    }

    private def sub(lhsValue: Long, rhsValue: Long, borrowed: Int = 0): SubtractResult = {
      if (needBorrowOnSub(lhsValue, rhsValue, borrowed)) {
        val borrow = 1
        // means: (Long.MaxValue + lhsValue - borrowed) - rhsValue
        val rest = Long.MaxValue - (rhsValue - lhsValue) - borrowed
        SubtractResult(borrow, rest)
      } else {
        val carry = 0
        val rest = lhsValue - rhsValue - borrowed
        SubtractResult(carry, rest)
      }
    }

    def +(rhs: uint256): Option[uint256] = {
      val fourthAddResult = add(lhs.fourth, rhs.fourth)
      val thirdAddResult = add(lhs.third, rhs.third, fourthAddResult.carry)
      val secondAddResult = add(lhs.second, rhs.second, thirdAddResult.carry)
      val firstAddResult = add(lhs.first, rhs.first, secondAddResult.carry)
      if (firstAddResult.carry == 1) {
        None // firstの繰上げはできない
      } else {
        Some(uint256(firstAddResult.rest, secondAddResult.rest, thirdAddResult.rest, fourthAddResult.rest))
      }
    }

    def -(rhs: uint256): Option[uint256] = {
      val fourthSubResult = sub(lhs.fourth, rhs.fourth)
      val thirdSubResult = sub(lhs.third, rhs.third, fourthSubResult.borrow)
      val secondSubResult = sub(lhs.second, rhs.second, thirdSubResult.borrow)
      val firstSubResult = sub(lhs.first, rhs.first, secondSubResult.borrow)
      if (firstSubResult.borrow == 1) {
        None // firstは繰り下げできない
      } else {
        Some(uint256(firstSubResult.rest, secondSubResult.rest, thirdSubResult.rest, fourthSubResult.rest))
      }
    }

    def isZero: Boolean = {
      // 全オクテットが0。
      lhs.first == 0 && lhs.second == 0 && lhs.third == 0 && lhs.fourth == 0
    }

    def isPositive: Boolean = {
      // 全オクテットが0以上かつ、どれかが0より大きい。
      lhs.first >= 0 && lhs.second >= 0 && lhs.third >= 0 && lhs.fourth >= 0 &&
        (lhs.first > 0 || lhs.second > 0 || lhs.third > 0 || lhs.fourth > 0)
    }
  }

}
