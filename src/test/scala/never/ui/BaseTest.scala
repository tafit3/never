package never.ui

import never.domain.TestDomain
import org.mockito.IdiomaticMockito.WithExpect
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.{OneInstancePerTest, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait BaseTest extends AnyWordSpec with MockitoSugar with Matchers with TestDomain with WithExpect with ArgumentMatchersSugar
  with OneInstancePerTest with OptionValues
